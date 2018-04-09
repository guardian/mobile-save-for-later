package com.gu.sfl.lambda

import java.io.{InputStream, OutputStream}

import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import com.gu.AwsIdentity
import com.gu.sfl.Logging
import com.gu.sfl.lib.SsmConfig
import com.gu.sfl.save.SaveForLaterControllerImpl

import scala.util.Try

abstract class AwsLambda(function: LambdaRequest => LambdaResponse) extends RequestStreamHandler with Logging {

  private val lambdaApiGateway = new LambdaApiGatewayImpl(function)

  override def handleRequest(input: InputStream, output: OutputStream, context: Context): Unit = Try(lambdaApiGateway.execute(input, output)).recover {
      case t: Throwable => logger.warn("Error executing lambda: ", t)
        throw t
    }
}

object SaveForLaterLambda extends Logging {

  lazy val ssmConfig = new SsmConfig("save-for-later")

  lazy val saveForLaterController: SaveForLaterControllerImpl = logOnThrown(
    () => ssmConfig.identity match {
      case awsIdentity: AwsIdentity => new SaveForLaterControllerImpl()
      case _ => throw new IllegalStateException("Unable to retrieve configuration")
    }, "Error initialising save for later controler")
}

class SaveForLaterLambda extends AwsLambda(SaveForLaterLambda.saveForLaterController)
