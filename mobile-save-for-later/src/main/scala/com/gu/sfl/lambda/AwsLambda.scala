package com.gu.sfl.lambda

import java.io.{InputStream, OutputStream}

import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import com.gu.sfl.Logging
import com.gu.sfl.lib.CloudWatchPublisher

import scala.concurrent.Future
import scala.util.Try

abstract class AwsLambda(function: LambdaRequest => Future[LambdaResponse], cloudWatchPublisher: CloudWatchPublisher) extends RequestStreamHandler with Logging {

  private val lambdaApiGateway = new LambdaApiGatewayImpl(function)

  override def handleRequest(input: InputStream, output: OutputStream, context: Context): Unit = {
    Try(lambdaApiGateway.execute(input, output)).recover {
      case t: Throwable => logger.warn("Error executing lambda: ", t)
        throw t
    }
    cloudWatchPublisher.sendMetricsSoFar()
  }
}

