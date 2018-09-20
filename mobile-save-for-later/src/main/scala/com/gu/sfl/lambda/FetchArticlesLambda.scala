package com.gu.sfl.lambda

import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClientBuilder
import com.gu.AwsIdentity
import com.gu.sfl.Logging
import com.gu.sfl.controller.FetchArticlesController
import com.gu.sfl.identity.{IdentityConfig, IdentityServiceImpl}
import com.gu.sfl.lib.Parallelism.largeGlobalExecutionContext
import com.gu.sfl.lib.{CloudWatchImpl, GlobalHttpClient, SsmConfig}
import com.gu.sfl.persistence.{PersistenceConfig, SavedArticlesPersistenceImpl}
import com.gu.sfl.savedarticles.FetchSavedArticlesImpl

object FetchArticlesLambda extends Logging {

  lazy val ssmConfig = new SsmConfig("save-for-later")

  lazy val savedArticledController: FetchArticlesController = logOnThrown(
    () => {
      ssmConfig.identity match {
        case awsIdentity: AwsIdentity =>
          logger.debug(s"Configuring controller with environment variables: Stack: ${awsIdentity.stack} Stage: ${awsIdentity.stage} App; ${awsIdentity.app}")
          val cloudWatchImpl = new CloudWatchImpl(awsIdentity.app, awsIdentity.stage, "fetch", AmazonCloudWatchAsyncClientBuilder.defaultClient())
          new FetchArticlesController(
            new FetchSavedArticlesImpl(
              new IdentityServiceImpl(IdentityConfig(ssmConfig.config.getString("identity.apiHost")), GlobalHttpClient.defaultHttpClient),
              new SavedArticlesPersistenceImpl(PersistenceConfig(awsIdentity.app, awsIdentity.stage), cloudWatchImpl)
            ), cloudWatchImpl
          )
        case _ => throw new IllegalStateException("Unable to retrieve configuration")
      }
    }, "Error initialising saved articles controller")
}

class FetchArticlesLambda extends AwsLambda(function = FetchArticlesLambda.savedArticledController)
