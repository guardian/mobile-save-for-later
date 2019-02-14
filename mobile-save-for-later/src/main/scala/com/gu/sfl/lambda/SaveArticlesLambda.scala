package com.gu.sfl.lambda

import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClientBuilder
import com.gu.sfl.Logging
import com.gu.sfl.controller.SaveArticlesController
import com.gu.sfl.identity.{IdentityConfig, IdentityServiceImpl}
import com.gu.sfl.lambda.SaveArticlesConfig.{app, stage}
import com.gu.sfl.lib.Parallelism.largeGlobalExecutionContext
import com.gu.sfl.lib.{CloudWatchImpl, GlobalHttpClient, SavedArticlesMergerConfig, SavedArticlesMergerImpl}
import com.gu.sfl.persistence.{PersistenceConfig, SavedArticlesPersistenceImpl}
import com.gu.sfl.savedarticles.UpdateSavedArticlesImpl
object SaveArticlesConfig {
  private val identityApihostKey = "IdentityApiHost"
  private val stageKey = "Stage"
  private val appKey = "App"
  private val savedArticleLimitKey = "SavedArticleLimit"

  lazy val identityApiHost = sys.env.getOrElse(identityApihostKey, throw new NullPointerException(identityApihostKey))
  lazy val app = sys.env.getOrElse(appKey, throw new NullPointerException(appKey))
  lazy val stage = sys.env.getOrElse(stageKey, throw new NullPointerException(stageKey))
  lazy val savedArticleLimit = sys.env.getOrElse(savedArticleLimitKey, throw new NullPointerException(savedArticleLimitKey)).toInt

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
