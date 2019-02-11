package com.gu.sfl.lambda

import java.io.{InputStream, OutputStream}
import java.nio.charset.StandardCharsets

import com.gu.sfl.Logging
import com.gu.sfl.lib.Jackson._
import com.gu.sfl.lib.Parallelism.largeGlobalExecutionContext
import com.gu.sfl.util.HeaderNames.acceptEncoding
import com.gu.sfl.util.SealedCompression.Compression
import com.gu.sfl.util.{HeaderNames, SealedCompression, StatusCodes}
import org.apache.commons.io.IOUtils

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object ApiGatewayLambdaResponse {
  val minimalHeaders: Map[String, String] = Map("Content-Type" -> "application/json; charset=UTF-8", "cache-control" -> "max-age=0")

  def apply(lambdaResponse: LambdaResponse, maybeCompression: Option[Compression]): ApiGatewayLambdaResponse = {
    val requestHeaders: Map[String, String] = minimalHeaders ++ lambdaResponse.headers
    maybeCompression match {
      case Some(compression) =>
        ApiGatewayLambdaResponse(lambdaResponse.statusCode, lambdaResponse.maybeBody.map(compression.encodeToBase64), requestHeaders + (HeaderNames.contentEncoding -> compression.contentEncoding), Some(true))
      case None => ApiGatewayLambdaResponse(lambdaResponse.statusCode, lambdaResponse.maybeBody, requestHeaders, None)
    }
  }

  def apply(statusCode: Int): ApiGatewayLambdaResponse = {
    ApiGatewayLambdaResponse(statusCode, None, minimalHeaders, None)
  }

}

object ApiGatewayLambdaRequest {

  def mapToOption[S, T](map: Map[S, T]): Option[Map[S, T]] = if (map.nonEmpty) Some(map) else None

  def apply(lambdaRequest: LambdaRequest, maybeCompression: Option[Compression]): ApiGatewayLambdaRequest = {
    (lambdaRequest.maybeBody, maybeCompression) match {
      case (Some(body), Some(compression)) => ApiGatewayLambdaRequest(
        body = Some(compression.encodeToBase64(body)),
        headers = Some(lambdaRequest.headers + (HeaderNames.contentEncoding -> compression.contentEncoding)),
        isBase64Encoded = Some(true)
      )
      case _ => ApiGatewayLambdaRequest(
        body = lambdaRequest.maybeBody,
        headers = mapToOption(lambdaRequest.headers),
        isBase64Encoded = None
      )
    }
  }
}

case class ApiGatewayLambdaRequest(
  body: Option[String],
  headers: Option[Map[String, String]],
  isBase64Encoded: Option[Boolean]
)

case class ApiGatewayLambdaResponse(
  statusCode: Int,
  body: Option[String],
  headers: Map[String, String],
  isBase64Encoded: Option[Boolean]
)


object LambdaRequest {

  def apply(apiGatewayLambdaRequest: ApiGatewayLambdaRequest): LambdaRequest = {
    val headers = apiGatewayLambdaRequest.headers.map { h => h.map { case (key, value) => (key.toLowerCase, value) } }.getOrElse(Map.empty)
    LambdaRequest(
      apiGatewayLambdaRequest.body.map(apiBody => {
        val maybeCompression = if (apiGatewayLambdaRequest.isBase64Encoded.contains(true)) {
          headers.get(HeaderNames.contentEncoding).flatMap(SealedCompression.contentEncodings.get)
        }
        else {
          None
        }
        maybeCompression match {
          case Some(compression) => compression.decodeFromBase64(apiBody)
          case _ => apiBody
        }
      }),
      headers,
    )
  }
}

case class LambdaRequest(
  maybeBody: Option[String],
  headers: Map[String, String]
)

object LambdaResponse {
  def apply(apiGatewayLambdaResponse: ApiGatewayLambdaResponse): LambdaResponse = {
    val headers = apiGatewayLambdaResponse.headers.map { case (key, value) => (key.toLowerCase, value) }
    apiGatewayLambdaResponse.body.map(apiBody => {
      val maybeCompression = if (apiGatewayLambdaResponse.isBase64Encoded.contains(true)) {
        headers.get(HeaderNames.contentEncoding).flatMap(SealedCompression.contentEncodings.get)
      }
      else {
        None
      }
      maybeCompression match {
        case Some(compression) => compression.decodeFromBase64(apiBody)
        case _ => apiBody
      }
    })

    LambdaResponse(apiGatewayLambdaResponse.statusCode, apiGatewayLambdaResponse.body, apiGatewayLambdaResponse.headers)
  }
}

case class LambdaResponse(
  statusCode: Int,
  maybeBody: Option[String],
  headers: Map[String, String] = Map("Content-Type" -> "application/json; charset=UTF-8", "cache-control" -> "max-age=0")
)


trait LambdaApiGateway {
  def execute(inputStream: InputStream, outputStream: OutputStream): Unit
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

  def getEncodings(acceptEncoding: String): List[String] = {
    def formatEncoding(encoding: String): Option[String] = {
      encoding.split(";q=", 2).toSeq.map(_.trim).headOption.filter(_.nonEmpty)
    }
    acceptEncoding.split(",").toSeq.map(_.trim).filter(_.nonEmpty).flatMap(formatEncoding).toList
  }

  override def execute(inputStream: InputStream, outputStream: OutputStream): Unit = {
    try {
      val response: Future[ApiGatewayLambdaResponse] = objectReadAndClose(inputStream) match {
        case Left(apiLambdaGatewayRequest) =>
          val lambdaRequest = LambdaRequest(apiLambdaGatewayRequest)
          function(lambdaRequest).map { res =>
            logger.debug(s"ApiGateway  lamda response: $res")
            val maybeCompression = for {
              header <- lambdaRequest.headers.get(acceptEncoding)
              encodings = getEncodings(header)
              firstCompression <- encodings.flatMap(SealedCompression.contentEncodings.get).headOption
            } yield firstCompression
            ApiGatewayLambdaResponse(res, maybeCompression)
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

