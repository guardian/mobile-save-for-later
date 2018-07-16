package com.gu.sfl.lambda

import com.gu.AwsIdentity
import com.gu.sfl.Logging
import com.gu.sfl.controller.FetchArticlesController
import com.gu.sfl.identity.{IdentityConfig, IdentityServiceImpl}
import com.gu.sfl.lib.GlobalHttpClient
import com.gu.sfl.persisitence.{PersistanceConfig, SavedArticlesPersistenceImpl}
import com.gu.sfl.savedarticles.FetchSavedArticlesImpl
import sfl.lib.SsmConfig

import scala.concurrent.ExecutionContext.Implicits.global

object FetchArticlesLambda extends Logging {

  lazy val ssmConfig = new SsmConfig("save-for-later")

  lazy val savedArticledController: FetchArticlesController = logOnThrown(
    () => {
      ssmConfig.identity match {
        case awsIdentity: AwsIdentity =>
          logger.debug(s"Configuring controller with environment variables: Stack: ${awsIdentity.stack} Stage: ${awsIdentity.stage} App; ${awsIdentity.app}")
          new FetchArticlesController(
            new FetchSavedArticlesImpl(
              new IdentityServiceImpl(IdentityConfig(ssmConfig.config.getString("identity.apiHost")),GlobalHttpClient.defaultHttpClient),
              new SavedArticlesPersistenceImpl( PersistanceConfig(awsIdentity.app, awsIdentity.stage) )
            )
          )
        case _ => throw new IllegalStateException("Unable to retrieve configuration")
      }}, "Error initialising saved articles controller")
}

class FetchArticlesLambda extends AwsLambda(function = FetchArticlesLambda.savedArticledController)
