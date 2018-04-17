package com.gu.sfl.lambda

import com.gu.AwsIdentity
import com.gu.sfl.Logging
import com.gu.sfl.controller.SavedArticlesController
import com.gu.sfl.lib.SsmConfig
import com.gu.sfl.persisitence.{PersistanceConfig, SavedArticlesPersistenceImpl}
import com.gu.sfl.services.{IdentityConfig, IdentityServiceImpl}

//TODO - BOTH lambdas need renaming
object SavedArticlesLambda extends Logging {

  lazy val ssmConfig = new SsmConfig("save-for-later")

  lazy val savedArticledController: SavedArticlesController = logOnThrown(
    () => {
      logger.info("Configuring controller")
      ssmConfig.identity match {
        case awsIdentity: AwsIdentity =>
            new SavedArticlesController(
              new IdentityServiceImpl(IdentityConfig(ssmConfig.config.getString("identity.apiHost"))),
              new SavedArticlesPersistenceImpl(PersistanceConfig(awsIdentity.app, awsIdentity.stage).tableName)
            )
        case _ => throw new IllegalStateException("Unable to retrieve configuration")
      }}, "Error initialising saved articles controller")
}

class SavedArticlesLambda extends AwsLambda(SavedArticlesLambda.savedArticledController)


