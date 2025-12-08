package com.gu.sfl.lambda

import cats.effect.IO

import java.io.{InputStream, OutputStream}
import java.nio.charset.StandardCharsets
import com.gu.sfl.Logging
import com.gu.sfl.lib.Base64Utils
import com.gu.sfl.lib.Jackson._
import com.gu.sfl.util.StatusCodes
import org.apache.commons.io.IOUtils
import com.gu.sfl.lib.Parallelism.largeGlobalExecutionContext
import fs2.text.utf8Encode
import org.http4s.{EmptyBody, EntityDecoder, Header, Headers, Request, Response, Status}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import org.typelevel.ci.CIString
import fs2.Stream
import org.http4s.Status.InternalServerError

object ApiGatewayLambdaResponse extends Base64Utils {
  def apply(lamdaResponse: LambdaResponse): ApiGatewayLambdaResponse = ApiGatewayLambdaResponse(lamdaResponse.statusCode, lamdaResponse.maybeBody, lamdaResponse.headers)
}

object ApiGatewayLambdaRequest extends Base64Utils {

  def mapToOption[S,T](map: Map[S,T]): Option[Map[S,T]] = if (map.nonEmpty) Some(map) else None

  def apply(lambdaRequest: LambdaRequest) : ApiGatewayLambdaRequest = {
    ApiGatewayLambdaRequest(
      body = lambdaRequest.maybeBody,
      headers = mapToOption(lambdaRequest.headers)
    )
  }
}

case class ApiGatewayLambdaRequest(
    body: Option[String],
    queryStringParameters: Option[Map[String, String]] = None,
    headers: Option[Map[String, String]] = None
)

case class ApiGatewayLambdaResponse (
    statusCode: Int,
    body: Option[String] = None,
    headers: Map[String, String] = Map("Content-Type" -> "application/json; charset=UTF-8", "cache-control" -> "max-age=0")
)


object LambdaRequest {
  def apply(apiGatewayLambdaRequest: ApiGatewayLambdaRequest): LambdaRequest = {
    val headers = apiGatewayLambdaRequest.headers.map{ h => h.map {case (key, value) => (key.toLowerCase, value)}}.getOrElse(Map.empty)
    LambdaRequest(apiGatewayLambdaRequest.body, headers.toMap)
  }

  def fromHttp4sRequest(request: Request[IO]) = {
    val headerToMapValue = (header: Header.Raw) => header.name.toString.toLowerCase -> header.value
    val headers = request.headers.headers.map(headerToMapValue(_)).toMap
    val body = EntityDecoder.decodeText(request).unsafeRunSync()
    LambdaRequest(Some(body), headers)
  }
}

case class LambdaRequest(maybeBody: Option[String], headers: Map[String, String] = Map.empty)

object LambdaResponse extends Base64Utils {
  def apply(apiGatewayLambdaResponse: ApiGatewayLambdaResponse) : LambdaResponse = {
    LambdaResponse(apiGatewayLambdaResponse.statusCode, apiGatewayLambdaResponse.body, apiGatewayLambdaResponse.headers)
  }
  def toHttp4sRes(lambdaResponse: LambdaResponse): Response[IO] = {
    Response(
      status = Status.fromInt(lambdaResponse.statusCode).getOrElse(InternalServerError),
      headers = Headers(lambdaResponse.headers.map({ case (k, v) => Header.Raw(CIString(k), v) }).toList),
      body = lambdaResponse.maybeBody.map(b => Stream(b).through(utf8Encode)).getOrElse(EmptyBody)
    )
  }
}

case class LambdaResponse(
  statusCode: Int,
  maybeBody: Option[String],
  headers: Map[String, String] = Map("Content-Type" -> "application/json; charset=UTF-8", "cache-control" -> "max-age=0")
)


trait LambdaApiGateway {
  def execute(inputStream: InputStream, outputStream: OutputStream) : Unit
}

class LambdaApiGatewayImpl(function: (LambdaRequest => Future[LambdaResponse])) extends LambdaApiGateway with Logging {

  def stringReadAndClose(inputStream: InputStream): String = {
    try {
         new String(IOUtils.toByteArray(inputStream), StandardCharsets.UTF_8)
    } finally {
      inputStream.close()
    }
  }

  private def objectReadAndClose(inputStream: InputStream): Either[ApiGatewayLambdaRequest, Throwable] = {
    val inputAsString = stringReadAndClose(inputStream)
    try {
      Left(mapper.readValue(inputAsString, classOf[ApiGatewayLambdaRequest]))
    } catch {
      case t: Throwable => logger.error(s"Input not an API gateway request: $inputAsString")
        Right(t)
    }
  }

  override def execute(inputStream: InputStream, outputStream: OutputStream): Unit = {
    try {
      val response: Future[ApiGatewayLambdaResponse] = objectReadAndClose(inputStream) match {
        case Left(apiLambdaGatewayRequest) =>
          function(LambdaRequest(apiLambdaGatewayRequest)).map { res =>
            logger.debug(s"ApiGateway lamda response: ${res}")
            ApiGatewayLambdaResponse(res)
          }
       case Right(error) =>
          logger.error("Lambda returned error", error)
          Future.successful(ApiGatewayLambdaResponse(StatusCodes.internalServerError))
      }

      val result = Await.result(response, 18.seconds) // with a buffer before lambda 20 second timeout
      mapper.writeValue(outputStream, result)
    }
    finally {
      outputStream.close()
    }
  }
}

