package com.gu.sfl.lambda

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import java.nio.charset.StandardCharsets.UTF_8

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import com.gu.sfl.lib.Jackson._
import LambdaApiGatewaySpec.stringAsInputStream

import scala.concurrent.Future

object LambdaApiGatewaySpec {
  def stringAsInputStream(s: String): InputStream = new ByteArrayInputStream(s.getBytes(UTF_8))
}

class LambdaApiGatewaySpec extends Specification with ScalaCheck {

  "LamdaApiGateWay" should {
    "marshal and and unmarshal a string properly" in {
      val outputStream: ByteArrayOutputStream = new ByteArrayOutputStream()
      val expectedBodyString = """{"test" : "content"}"""
      val expectedBodyJson = mapper.readTree(expectedBodyString)
      val inputStream =  stringAsInputStream("""{"body":"{\"test\":\"content\"}","isBase64Encoded":false,"queryStringParameters":{"Content-Type":"application/json; charset=UTF-8"}}""")


      new LambdaApiGatewayImpl((lambdaRequest: LambdaRequest) => {
        lambdaRequest.queryStringParameters must beEqualTo(Map("Content-Type" -> "application/json; charset=UTF-8"))

        lambdaRequest.maybeBody match {
          case Some(body) =>  mapper.readTree(body) must beEqualTo(expectedBodyJson)
          case notString => notString must beEqualTo(Some(expectedBodyJson))
        }
        Future.successful(LambdaResponse(200, Some("""{"test":"body"}"""), Map("Content-Type" -> "application/json; charset=UTF-8")))
      }).execute(
        inputStream, outputStream
      )

      mapper.readTree(outputStream.toByteArray) must beEqualTo(mapper.readTree(
        """{"statusCode":200,"isBase64Encoded":false,"headers":{"Content-Type":"application/json; charset=UTF-8"},"body":"{\"test\":\"body\"}"}"""
      ))
    }

    //Todo We're going to have to handle bytes
    "marshal and unmarshal bytes properly" in {
      val outputStream: ByteArrayOutputStream = new ByteArrayOutputStream()
      val inputStream = stringAsInputStream("""{"body":"dGVzdEJhc2U2NGlucHV0","isBase64Encoded":true,"queryStringParameters":{"Content-Type":"text/plain"}}""")
      new LambdaApiGatewayImpl((lambdaRequest: LambdaRequest) => {
        throw new IllegalStateException("This should not be called. For now")
      }).execute(inputStream, outputStream)

      mapper.readTree(outputStream.toByteArray) must beEqualTo(mapper.readTree(
        """{"statusCode":400,"body":"Binary content not supported","headers":{"Content-Type":"text/plain"},"isBase64Encoded":false}"""
      ))
    }
  }


}
