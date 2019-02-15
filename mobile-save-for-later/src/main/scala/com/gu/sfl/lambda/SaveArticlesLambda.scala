package com.gu.sfl.lambda

import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClientBuilder
import com.gu.sfl.Logging
import com.gu.sfl.controller.SaveArticlesController
import com.gu.sfl.identity.{IdentityConfig, IdentityServiceImpl}
import com.gu.sfl.lambda.AwsLambda.readSystemKey
import com.gu.sfl.lambda.SaveArticlesConfig.{app, stage}
import com.gu.sfl.lib.Parallelism.largeGlobalExecutionContext
import com.gu.sfl.lib.{CloudWatchImpl, GlobalHttpClient, SavedArticlesMergerConfig, SavedArticlesMergerImpl}
import com.gu.sfl.persistence.{PersistenceConfig, SavedArticlesPersistenceImpl}
import com.gu.sfl.savedarticles.UpdateSavedArticlesImpl
object SaveArticlesConfig {

  lazy val identityApiHost: String = readSystemKey("IdentityApiHost")
  lazy val app: String = readSystemKey("App")
  lazy val stage: String = readSystemKey("Stage")
  lazy val savedArticleLimit: Int = readSystemKey("SavedArticleLimit").toInt

}
object SaveArticlesLambda extends Logging {
  lazy val cloudwatch = new CloudWatchImpl(app, stage, "save", AmazonCloudWatchAsyncClientBuilder.defaultClient())

  lazy val saveForLaterController: SaveArticlesController = logOnThrown(
    () => {
          new SaveArticlesController(
            new UpdateSavedArticlesImpl(
              new IdentityServiceImpl(IdentityConfig(SaveArticlesConfig.identityApiHost), GlobalHttpClient.defaultHttpClient),
              new SavedArticlesMergerImpl(SavedArticlesMergerConfig(SaveArticlesConfig.savedArticleLimit),
                new SavedArticlesPersistenceImpl(PersistenceConfig(app, stage), cloudwatch)
              )
            )
          )
    }, "Error initialising save for later controller")
}


class SaveArticlesLambda extends AwsLambda(SaveArticlesLambda.saveForLaterController, SaveArticlesLambda.cloudwatch)
