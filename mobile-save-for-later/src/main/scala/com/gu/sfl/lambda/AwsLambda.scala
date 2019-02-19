package com.gu.sfl.lambda

import java.io.{InputStream, OutputStream}

import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import com.gu.sfl.Logging

import scala.concurrent.Future
import scala.util.Try

object AwsLambda {
  def readEnvKey(key: String): String = sys.env.getOrElse(key, throw new NullPointerException(s"Couldn't load environment variable $key"))
}
abstract class AwsLambda(function: LambdaRequest => Future[LambdaResponse]) extends RequestStreamHandler with Logging {

  private val lambdaApiGateway = new LambdaApiGatewayImpl(function)

  override def handleRequest(input: InputStream, output: OutputStream, context: Context): Unit = {
    Try(lambdaApiGateway.execute(input, output)).recover {
      case t: Throwable => logger.warn("Error executing lambda: ", t)
        throw t
    }
  }
}

