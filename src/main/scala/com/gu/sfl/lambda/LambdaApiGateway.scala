package com.gu.sfl.lambda

import java.io.{InputStream, OutputStream}

import com.gu.sfl.lib.Base64Utils
import com.gu.sfl.lib.Jackson._

object ApiGatewayLambdaResponse extends Base64Utils {
  def apply(lamdaResponse: LambdaResponse): ApiGatewayLambdaResponse =
    lamdaResponse.maybeBody match {
      case Some(body) => body match {
        case Left(str) => ApiGatewayLambdaResponse(lamdaResponse.statusCode, Some(str), lamdaResponse.headers, false)
        case Right(bytes) => ApiGatewayLambdaResponse(lamdaResponse.statusCode, Some(encodeByeArray(bytes)), lamdaResponse.headers, true)
      }
      case None => ApiGatewayLambdaResponse(lamdaResponse.statusCode, None, lamdaResponse.headers)
    }

  def foundBody(apiGatewayLambdaResponse: ApiGatewayLambdaResponse) : Option[Either[String, Array[Byte]]] = apiGatewayLambdaResponse.maybeBody.map {
    body => if(apiGatewayLambdaResponse.isBase64Encoded) Right(decoder.decode(body)) else Left(body)
  }
}

object ApiGatewayLambdaRequest extends Base64Utils {
  def apply(lambdaRequest: LambdaRequest) : ApiGatewayLambdaRequest = lambdaRequest.maybeBody match {
    case Some(body) => body match{
      case Left(str) => ApiGatewayLambdaRequest(Some(str), false)
      case Right(bytes) => ApiGatewayLambdaRequest(Some(encodeByeArray(bytes)), true)
    }
    case None => ApiGatewayLambdaRequest(None)
  }

  def foundBody(apiGatewayLambdaRequest: ApiGatewayLambdaRequest) : Option[Either[String, Array[Byte]]] = apiGatewayLambdaRequest.body.map {
      body => if(apiGatewayLambdaRequest.isBase64Encoded) Right(decoder.decode(body)) else Left(body)
  }
}

case class ApiGatewayLambdaRequest(body: Option[String], isBase64Encoded: Boolean = false)

case class ApiGatewayLambdaResponse (
  statusCode: Int,
  maybeBody: Option[String] = None,
  headers: Map[String, String] = Map("Content-Type" -> "application/json"),
  isBase64Encoded: Boolean = false
)


object LambdaRequest {
  def apply(apiGatewayLambdaRequest: ApiGatewayLambdaRequest): LambdaRequest = {
    val body = ApiGatewayLambdaRequest.foundBody(apiGatewayLambdaRequest)
    LambdaRequest(body)
  }
}

case class LambdaRequest(maybeBody: Option[Either[String, Array[Byte]]])


//Todo if we don't ever need to encode the response, we don't need any of this shizzle
object LambdaResponse extends Base64Utils {
  def apply(apiGatewayLambdaResponse: ApiGatewayLambdaResponse) : LambdaResponse = {
    val body = ApiGatewayLambdaResponse.foundBody(apiGatewayLambdaResponse)
    LambdaResponse(apiGatewayLambdaResponse.statusCode, body, apiGatewayLambdaResponse.headers)
  }
}

case class LambdaResponse(
  statusCode: Int,
  maybeBody: Option[Either[String, Array[Byte]]],
  headers: Map[String, String] = Map("Content-Type" -> "application/json"))


trait LambdaApiGateway {
  def execute(inputStream: InputStream, outputStream: OutputStream, function: (LambdaRequest => LambdaResponse))
}

class LambdaApiGatewayImpl extends LambdaApiGateway {
  override def execute(inputStream: InputStream, outputStream: OutputStream, function: LambdaRequest => LambdaResponse): Unit = ???
}

