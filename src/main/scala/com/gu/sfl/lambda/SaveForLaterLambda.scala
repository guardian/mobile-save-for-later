package com.gu.sfl.lambda

import com.gu.AwsIdentity
import com.gu.sfl.Logging
import com.gu.sfl.controller.SaveForLaterControllerImpl
import com.gu.sfl.lambda.SaveForLaterLambda.{logOnThrown, logger}
import com.gu.sfl.lambda.SavedArticlesLambda.ssmConfig
import com.gu.sfl.lib._
import com.gu.sfl.persisitence.{PersistanceConfig, SavedArticlesPersistenceImpl}
import com.gu.sfl.savedarticles.UpdateSavedArticlesImpl
import com.gu.sfl.services.{IdentityConfig, IdentityServiceImpl}

object SaveForLaterLambda extends Logging {

  lazy val ssmConfig = new SsmConfig("save-for-later")

  lazy val saveForLaterController: SaveForLaterControllerImpl = logOnThrown(
    () => {
      logger.info("Configuring controller")
      ssmConfig.identity match {
        case awsIdentity: AwsIdentity =>
          new SaveForLaterControllerImpl(
            new UpdateSavedArticlesImpl(
              new IdentityServiceImpl(IdentityConfig(ssmConfig.config.getString("identity.apiHost")), GlobalHttpClient.defaultHttpClient),
              new SavedArticlesMergerImpl( SavedArticlesMergerConfig(ssmConfig.config.getInt("savedarticle.limit")),
                new SavedArticlesPersistenceImpl( PersistanceConfig(awsIdentity.app, awsIdentity.stage) )
              )
            )
          )
        case _ => throw new IllegalStateException("Unable to retrieve configuration")
      }}, "Error initialising save for later controller")
}

class SaveForLaterLambda extends AwsLambda(SaveForLaterLambda.saveForLaterController)
