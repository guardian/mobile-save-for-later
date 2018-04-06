package com.gu.sfl.lambda

import java.io.{InputStream, OutputStream}

import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import com.gu.sfl.Logging
import com.gu.sfl.save.{SaveForLaterController, SaveForLaterControllerImpl}

abstract class SaveForLaterLambda(lambdaApiGateway: LambdaApiGateway, saveForLaterController: SaveForLaterController) extends RequestStreamHandler with Logging {

  override def handleRequest(input: InputStream, output: OutputStream, context: Context): Unit = {
    try {
      lambdaApiGateway.execute(input, output, saveForLaterController.save)
    }
    catch {
      case t: Throwable => logger.warn("Error executing lambda: ", t)
    }
  }
}

object ConfiguredSaveForLaterLambda extends Logging {
  lazy val saveForLaterController: SaveForLaterController = logOnThrown(
    () => new SaveForLaterControllerImpl(), "Error initialising save for later controler"
  )

  lazy val lambdaApiGateway: LambdaApiGateway = logOnThrown(
    () => new LambdaApiGatewayImpl(), "Error initialising LambdaApiGateway"
  )
}

class ConfiguredSaveForLaterLambda extends SaveForLaterLambda(
  ConfiguredSaveForLaterLambda.lambdaApiGateway,
  ConfiguredSaveForLaterLambda.saveForLaterController
_
