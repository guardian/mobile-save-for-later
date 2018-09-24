package com.gu.sfl.lambda


import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.gu.sfl.lib.CloudWatchPublisher
import org.apache.logging.log4j.core.Logger
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.util.Try

class AwsLambdaSpec extends Specification with Mockito {

    "AswLambda" should {
      "Log but not error in" in {
        val mockedLogger = mock[Logger]
        val testException = new IllegalStateException("This is totally illegal")
        val lambda = new AwsLambda((_: LambdaRequest) => throw testException, new CloudWatchPublisher {
          override def sendMetricsSoFar(): Unit = ()
        }) {
          override val logger = mockedLogger

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
