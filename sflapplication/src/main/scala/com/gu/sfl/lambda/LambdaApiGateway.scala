package com.gu.sfl.lambda

import java.io.{InputStream, OutputStream}
import java.nio.charset.StandardCharsets

import com.gu.sfl.Logging
import com.gu.sfl.lib.Base64Utils
import org.apache.commons.io.IOUtils
import com.gu.sfl.lib.Jackson._
import com.gu.sfl.util.StatusCodes

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

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
}

case class LambdaRequest(maybeBody: Option[String], headers: Map[String, String] = Map.empty)

object LambdaResponse extends Base64Utils {
  def apply(apiGatewayLambdaResponse: ApiGatewayLambdaResponse) : LambdaResponse = {
    LambdaResponse(apiGatewayLambdaResponse.statusCode, apiGatewayLambdaResponse.body, apiGatewayLambdaResponse.headers)
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
            logger.debug(s"ApiGateway  lamda response: ${res}")
            ApiGatewayLambdaResponse(res)
          }
       case Right(_) =>
          logger.debug("Lambda returned error")
          Future.successful(ApiGatewayLambdaResponse(StatusCodes.internalServerError))
      }

      val result = Await.result(response, Duration.Inf)
      mapper.writeValue(outputStream, result)
    }
    finally {
      outputStream.close()
    }
  }
}

