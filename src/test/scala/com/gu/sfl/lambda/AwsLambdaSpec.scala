package com.gu.sfl.lambda

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.gu.sfl.Logging
import org.slf4j.Logger
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.util.Try

//TODO purchases crab. Extract
class AwsLambdaSpec extends Specification with Mockito {

    "AswLambda" should {
      "Log but not error in" in {
        val mockedLogger = mock[Logger]
        val testException = new IllegalStateException("This is totally illegal")
        val lambda = new AwsLambda((_: LambdaRequest) => throw testException) {
          override def logger: Logger = mockedLogger

        }
        Try(lambda.handleRequest(
          new ByteArrayInputStream("""{"body":"anybody","isBase64Encoded":false,"headers":{"Content-Type":"text/plain"}}""".getBytes()),
          new ByteArrayOutputStream(), null)
        ).recover {
          case t => t must beEqualTo(testException)
        }
        there was one(mockedLogger).warn(s"Error executing lambda: ", testException)

      }
    }


}
