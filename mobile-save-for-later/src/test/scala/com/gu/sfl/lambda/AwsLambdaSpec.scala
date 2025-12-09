package com.gu.sfl.lambda


import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.concurrent.TimeoutException

import org.slf4j.Logger
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.concurrent.Promise
import scala.util.Try

class AwsLambdaSpec extends Specification with Mockito {

    "AwsLambda" should {
      "log and throw exceptions" in {
        val testInput = new ByteArrayInputStream("""{"body":"anybody","isBase64Encoded":false,"headers":{"Content-Type":"text/plain"}}""".getBytes())
        val testOutput = new ByteArrayOutputStream()
        val mockedLogger = mock[Logger]
        val runtimeException = new RuntimeException("error")
        val lambda = new AwsLambda((_: LambdaRequest) => throw runtimeException) {
          override val logger = mockedLogger
        }

        lambda.handleRequest(testInput, testOutput, null) must throwA[RuntimeException]

        there was one(mockedLogger).error(
          org.mockito.ArgumentMatchers.contains("Error executing lambda: java.lang.RuntimeException")
        )
      }
    }
}
