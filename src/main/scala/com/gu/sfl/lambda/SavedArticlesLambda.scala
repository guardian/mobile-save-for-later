package com.gu.sfl.lambda

import com.gu.AwsIdentity
import com.gu.sfl.Logging
import com.gu.sfl.controller.SavedArticlesController
import com.gu.sfl.lib.{GlobalHttpClient, SsmConfig}
import com.gu.sfl.persisitence.{PersistanceConfig, SavedArticlesPersistenceImpl}
import com.gu.sfl.savedarticles.FetchSavedArticlesImpl
import com.gu.sfl.services.{IdentityConfig, IdentityServiceImpl}

//TODO - BOTH lambdas need renaming
object SavedArticlesLambda extends Logging {

  lazy val ssmConfig = new SsmConfig("save-for-later")

  lazy val savedArticledController: SavedArticlesController = logOnThrown(
    () => {
      logger.info("Configuring controller")
      ssmConfig.identity match {
        case awsIdentity: AwsIdentity =>
            logger.info(s"Configuring controller with environment variables: Stack: ${awsIdentity.stack} Stage: ${awsIdentity.stage} App; ${awsIdentity.app}")
            new SavedArticlesController(
              new FetchSavedArticlesImpl(
                new IdentityServiceImpl(IdentityConfig(ssmConfig.config.getString("identity.apiHost")),GlobalHttpClient.defaultHttpClient),
                new SavedArticlesPersistenceImpl( PersistanceConfig(awsIdentity.app, awsIdentity.stage) )
              )
            )
        case _ => throw new IllegalStateException("Unable to retrieve configuration")
      }}, "Error initialising saved articles controller")
}

class SavedArticlesLambda extends AwsLambda(SavedArticlesLambda.savedArticledController)


