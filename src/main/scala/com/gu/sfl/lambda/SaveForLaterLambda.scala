package com.gu.sfl.lambda

import com.gu.AwsIdentity
import com.gu.sfl.Logging
import com.gu.sfl.controller.SaveForLaterControllerImpl
import com.gu.sfl.lambda.SaveForLaterLambda.{logOnThrown, logger}
import com.gu.sfl.lib.SsmConfig
import com.gu.sfl.persisitence.{PersistanceConfig, SavedArticlesPersistenceImpl}

object SaveForLaterLambda extends Logging {

  lazy val ssmConfig = new SsmConfig("save-for-later")

  lazy val saveForLaterController: SaveForLaterControllerImpl = logOnThrown(
    () => {
      logger.info("Configuring controller")
      ssmConfig.identity match {
        case awsIdentity: AwsIdentity =>
          new SaveForLaterControllerImpl(new SavedArticlesPersistenceImpl(PersistanceConfig(awsIdentity.app, awsIdentity.stage).tableName))
        case _ => throw new IllegalStateException("Unable to retrieve configuration")
      }}, "Error initialising save for later controler")
}

class SaveForLaterLambda extends AwsLambda(SaveForLaterLambda.saveForLaterController)
