package com.gu.sfl.lambda


import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.concurrent.TimeoutException

import org.slf4j.Logger
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.util.Try

class AwsLambdaSpec extends Specification with Mockito {

    "AwsLambda" should {
      "log and throw exceptions" in {
        val mockedLogger = mock[Logger]
        val testException = new RuntimeException("RuntimeException")
        val lambda = new AwsLambda((_: LambdaRequest) => throw testException) {
          override val logger = mockedLogger
        }

        lambda.handleRequest(
          new ByteArrayInputStream("""{"body":"anybody","isBase64Encoded":false,"headers":{"Content-Type":"text/plain"}}""".getBytes()),
          new ByteArrayOutputStream(), null
        ) must throwA[RuntimeException]("RuntimeException")

        there was one(mockedLogger).error(argThat((msg: String) =>
          msg.startsWith("Error executing lambda:") && msg.contains("RuntimeException")
        ))
      }

      "log and throw TimeoutException when Await times out" in {
        val mockedLogger = mock[Logger]
        val testException = new TimeoutException("TimeoutException")
        val lambda = new AwsLambda((_: LambdaRequest) => throw testException) {
          override val logger = mockedLogger
        }

        lambda.handleRequest(
          new ByteArrayInputStream("""{"body":"anybody","isBase64Encoded":false,"headers":{"Content-Type":"text/plain"}}""".getBytes()),
          new ByteArrayOutputStream(), null
        ) must throwA[TimeoutException]

        there was one(mockedLogger).error(argThat((msg: String) =>
          msg.startsWith("Error executing lambda:") && msg.contains("TimeoutException")
        ))
      }
    }


}
