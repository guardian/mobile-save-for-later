package com.gu.sfl.lambda

import java.io.{InputStream, OutputStream}
import java.nio.charset.StandardCharsets

import com.gu.sfl.Logging
import com.gu.sfl.lib.Base64Utils
import com.gu.sfl.lib.Jackson._
import com.gu.sfl.util.StatusCodes
import org.apache.commons.io.IOUtils

import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

object ApiGatewayLambdaResponse extends Base64Utils {
  def apply(lamdaResponse: LambdaResponse): ApiGatewayLambdaResponse = ApiGatewayLambdaResponse(lamdaResponse.statusCode, None, lamdaResponse.headers)

  def foundBody(apiGatewayLambdaResponse: ApiGatewayLambdaResponse) : Option[String] = apiGatewayLambdaResponse.body.map {
    body => if(apiGatewayLambdaResponse.isBase64Encoded)
        throw new IllegalArgumentException("Binary content unsupported")
    else body
  }
}

object ApiGatewayLambdaRequest extends Base64Utils {

  def mapToOption[S,T](map: Map[S,T]) = if (map.isEmpty) Some(map) else None

  def apply(lambdaRequest: LambdaRequest) : ApiGatewayLambdaRequest = {
    ApiGatewayLambdaRequest(
      body = lambdaRequest.maybeBody,
      queryStringParameters = mapToOption(lambdaRequest.queryStringParameters),
      headers = mapToOption(lambdaRequest.headers)
    )

  }
  def foundBody(apiGatewayLambdaRequest: ApiGatewayLambdaRequest) : Option[String] = apiGatewayLambdaRequest.body.map {
    body => if(apiGatewayLambdaRequest.isBase64Encoded)
      throw new IllegalArgumentException("Binary content unsupported")
    else body
  }
}

case class ApiGatewayLambdaRequest(
    body: Option[String],
    isBase64Encoded: Boolean = false,
    queryStringParameters: Option[Map[String, String]] = None,
    headers: Option[Map[String, String]] = None
)

case class ApiGatewayLambdaResponse (
    statusCode: Int,
    body: Option[String] = None,
    headers: Map[String, String] = Map("Content-Type" -> "application/json"),
    isBase64Encoded: Boolean = false
)


object LambdaRequest {
  def apply(apiGatewayLambdaRequest: ApiGatewayLambdaRequest): LambdaRequest = {
    val body = ApiGatewayLambdaRequest.foundBody(apiGatewayLambdaRequest)
    val queryStringParams = apiGatewayLambdaRequest.queryStringParameters.getOrElse(Map.empty)
    val headers = apiGatewayLambdaRequest.headers.getOrElse(Map.empty)
    LambdaRequest(body, queryStringParams, headers)
  }
}

case class LambdaRequest(maybeBody: Option[String], queryStringParameters: Map[String, String] = Map.empty, headers: Map[String, String] = Map.empty)


//Todo if we don't ever need to encode the response, we don't need any of this shizzle
object LambdaResponse extends Base64Utils {
  def apply(apiGatewayLambdaResponse: ApiGatewayLambdaResponse) : LambdaResponse = {
    val body = ApiGatewayLambdaResponse.foundBody(apiGatewayLambdaResponse)
    LambdaResponse(apiGatewayLambdaResponse.statusCode, body, apiGatewayLambdaResponse.headers)
  }
}

case class LambdaResponse(
  statusCode: Int,
  maybeBody: Option[String],
  headers: Map[String, String] = Map("Content-Type" -> "application/json"))


trait LambdaApiGateway {
  def execute(inputStream: InputStream, outputStream: OutputStream)
}

class LambdaApiGatewayImpl(function: (LambdaRequest => Future[LambdaResponse])) extends LambdaApiGateway with Logging {

  def stringReadAndClose(inputStream: InputStream): String = {
    try {
        val inputAsString = new String(IOUtils.toByteArray(inputStream), StandardCharsets.UTF_8)
        logger.info(s"Input as string: ${inputAsString}")
        inputAsString
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

  //TODO shit name change it
  private def marshallResult(apiGatewayLambdaRequest: ApiGatewayLambdaRequest) : ApiGatewayLambdaResponse = {

      logger.info("Making lambda request")

      val promiseLambdaResponse: Promise[LambdaResponse] = Promise[LambdaResponse]
      val futureLambdaResponse: Future[LambdaResponse] = promiseLambdaResponse.future

      function(LambdaRequest(apiGatewayLambdaRequest)).transform((triedLambdaResponse: Try[LambdaResponse]) => {
        promiseLambdaResponse.complete(triedLambdaResponse)
        triedLambdaResponse
      })

      Await.ready(futureLambdaResponse, Duration.Inf)

      futureLambdaResponse.value.getOrElse(Failure(new IllegalStateException("Bark! Bark"))) match {
        case Success(lambdaResponse) =>
          logger.info(s"Got Result: $lambdaResponse")
          ApiGatewayLambdaResponse(lambdaResponse)
        case Failure(t) =>
          logger.info(s"lambda returned error")
          ApiGatewayLambdaResponse(StatusCodes.internalServerError)
      }

  }

  override def execute(inputStream: InputStream, outputStream: OutputStream): Unit = {
      logger.info("ApiGateway: Execute")

      try {
        mapper.writeValue(outputStream, objectReadAndClose(inputStream) match {
          case Left(apiGatewayLambdaRequest) =>
            logger.info("Read request OK")
            marshallResult(apiGatewayLambdaRequest)
          case Right(_) =>
            logger.info("Error reading request")
            ApiGatewayLambdaResponse(StatusCodes.internalServerError)
        })
        
      } finally {
        logger.info("ApiGateway: closing output")
        outputStream.close()
      }

  }

  def XXXexecute(inputStream: InputStream, outputStream: OutputStream): Unit = {
    logger.info("ApiGateway: Execute")
    try {
      val response: Future[ApiGatewayLambdaResponse] = objectReadAndClose(inputStream) match {
        case Left(apiLambdaGatewayRequest) =>
          function(LambdaRequest(apiLambdaGatewayRequest)).map { res =>
            logger.info(s"ApiGateway  lamda response: ${res}")
            ApiGatewayLambdaResponse(res)
          }
        case Right(_) =>
          logger.info("Lambda returned error")
          Future.successful(ApiGatewayLambdaResponse(StatusCodes.internalServerError))
      }

      val result = Await.result(response, Duration(6000, MILLISECONDS))

      logger.info(s"After response: ${result}" )
      mapper.writeValue(outputStream, result)
    }
    finally {
      outputStream.close()
    }
  }
}

