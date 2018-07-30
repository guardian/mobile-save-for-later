package com.gu.sfl.lambda

import com.gu.AwsIdentity
import com.gu.sfl.Logging
import com.gu.sfl.controller.SaveArticlesController
import com.gu.sfl.identity.{IdentityConfig, IdentityServiceImpl}
import com.gu.sfl.lib.{GlobalHttpClient, SavedArticlesMergerConfig, SavedArticlesMergerImpl, SsmConfig}
import com.gu.sfl.persistence.{PersistenceConfig, SavedArticlesPersistenceImpl}
import com.gu.sfl.savedarticles.UpdateSavedArticlesImpl

import scala.concurrent.ExecutionContext.Implicits.global

object SaveArticlesLambda extends Logging {

  lazy val ssmConfig = new SsmConfig("save-for-later")

  lazy val saveForLaterController: SaveArticlesController = logOnThrown(
    () => {
      logger.info("Configuring controller")
      ssmConfig.identity match {
        case awsIdentity: AwsIdentity =>
          new SaveArticlesController(
            new UpdateSavedArticlesImpl(
              new IdentityServiceImpl(IdentityConfig(ssmConfig.config.getString("identity.apiHost")), GlobalHttpClient.defaultHttpClient),
              new SavedArticlesMergerImpl( SavedArticlesMergerConfig(ssmConfig.config.getInt("savedarticle.limit")),
                new SavedArticlesPersistenceImpl( PersistenceConfig(awsIdentity.app, awsIdentity.stage) )
              )
            )
          )
        case _ => throw new IllegalStateException("Unable to retrieve configuration")
      }}, "Error initialising save for later controller")
}


class SaveArticlesLambda extends AwsLambda(SaveArticlesLambda.saveForLaterController)
