package com.gu.sfl.lambda


import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.charset.StandardCharsets

import com.gu.sfl.lib.{CloudWatchPublisher, Jackson}
import org.apache.logging.log4j.core.Logger
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

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
      "Gzip" in {
        val lambda = new AwsLambda(lambdaRequest=> {

          Try {
            lambdaRequest.maybeBody must beEqualTo((Some("anybody")))
            LambdaResponse(200, Some("anybody"))
          } match {
            case Failure(exception) => Future.failed(exception)
            case Success(x) => Future.successful(x)
          }


        }, new CloudWatchPublisher {
          override def sendMetricsSoFar(): Unit = ()
        }) {}
        val stream = new ByteArrayOutputStream()
        lambda.handleRequest(
          new ByteArrayInputStream("""{"body":"H4sIAH0cW1wAA0vMq0zKT6kEABWgfYwHAAAA","isBase64Encoded":true,"headers":{"Content-Type":"text/plain","Accept-Encoding":"gzip", "Content-Encoding":"gzip"}}""".getBytes()),
          stream, null)
        Jackson.mapper.readTree(new String(stream.toByteArray,StandardCharsets.UTF_8)) must beEqualTo(Jackson.mapper.readTree("""{"statusCode":200,"body":"H4sIAAAAAAAAAEvMq0zKT6kEABWgfYwHAAAA","headers":{"Content-Type":"application/json; charset=UTF-8","cache-control":"max-age=0","content-encoding":"gzip"},"isBase64Encoded":true}"""))

      }
    }


}
