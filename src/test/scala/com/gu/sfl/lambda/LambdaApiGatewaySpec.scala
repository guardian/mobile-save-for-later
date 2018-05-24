package com.gu.sfl.lambda

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import java.nio.charset.StandardCharsets.UTF_8

import com.gu.sfl.lambda.LambdaApiGatewaySpec.stringAsInputStream
import com.gu.sfl.lib.Jackson._
import com.gu.sfl.util.ScalaCheckUtils.genCommonAscii
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scala.concurrent.Future

object LambdaApiGatewaySpec {
  def stringAsInputStream(s: String): InputStream = new ByteArrayInputStream(s.getBytes(UTF_8))
}

class LambdaApiGatewaySpec extends Specification with ScalaCheck {

  private val genStringMap: Gen[(String, String)]  = Gen.zip(genCommonAscii, genCommonAscii)

  implicit def arbitraryLambdaRequest: Arbitrary[LambdaRequest] = Arbitrary( for{
    stringBinary <- Gen.option[String](genCommonAscii)
    headers <- Gen.mapOf[String, String](genStringMap)
  } yield LambdaRequest(stringBinary, headers) )

  implicit def arbitraryLambdaResponse: Arbitrary[LambdaResponse] = Arbitrary( for{
    statusCode <- Arbitrary.arbitrary[Int]
    stringBinary <- Gen.option[String](genCommonAscii)
    headers <- Gen.mapOf[String, String](genStringMap)
  } yield LambdaResponse(statusCode, stringBinary, headers) )


  "LamdaApiGateWay" should {
    "marshal and and unmarshal a string properly" in {
      val outputStream: ByteArrayOutputStream = new ByteArrayOutputStream()
      val expectedBodyString = """{"test" : "content"}"""
      val expectedBodyJson = mapper.readTree(expectedBodyString)
      val inputStream =  stringAsInputStream("""{"body":"{\"test\":\"content\"}","isBase64Encoded":false,"headers":{"Content-Type":"application/json; charset=UTF-8"}}""")


      new LambdaApiGatewayImpl((lambdaRequest: LambdaRequest) => {
        lambdaRequest.headers must beEqualTo(Map("Content-Type" -> "application/json; charset=UTF-8"))

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

    "check random lambdaRequest and LambdaResponse convert as expected" >> prop { (lambdaRequest: LambdaRequest, lambdaResponse: LambdaResponse) =>
      val outputStream: ByteArrayOutputStream = new ByteArrayOutputStream
      new LambdaApiGatewayImpl((request: LambdaRequest) => {
        request must beEqualTo(lambdaRequest)
        Future.successful(lambdaResponse)
      }).execute(
        stringAsInputStream(mapper.writeValueAsString(ApiGatewayLambdaRequest(lambdaRequest))),
        outputStream
      )
      mapper.readValue[ApiGatewayLambdaResponse](outputStream.toByteArray) must_== ApiGatewayLambdaResponse(lambdaResponse)
    }

    "lambda request sound across api request in" >> prop { (lambdaRequest: LambdaRequest) => {
        val renderedRequest: LambdaRequest = LambdaRequest(ApiGatewayLambdaRequest(lambdaRequest))
        renderedRequest must beEqualTo(lambdaRequest)
        renderedRequest.hashCode() must beEqualTo(lambdaRequest.hashCode())
      }
    }

    "lambda response sound across api request in" >> prop { (lambdaResponse: LambdaResponse) => {
        val renderedResponse: LambdaResponse = LambdaResponse(ApiGatewayLambdaResponse(lambdaResponse))
        renderedResponse must beEqualTo(lambdaResponse)
        renderedResponse.hashCode() must beEqualTo(lambdaResponse.hashCode())
      }
    }
  }

}
