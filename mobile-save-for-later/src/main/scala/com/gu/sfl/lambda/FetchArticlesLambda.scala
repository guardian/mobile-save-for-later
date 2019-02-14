package com.gu.sfl.lambda

import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClientBuilder
import com.gu.sfl.Logging
import com.gu.sfl.controller.FetchArticlesController
import com.gu.sfl.identity.{IdentityConfig, IdentityServiceImpl}
import com.gu.sfl.lambda.FetchArticlesConfig.{app, stage}
import com.gu.sfl.lib.Parallelism.largeGlobalExecutionContext
import com.gu.sfl.lib.{CloudWatchImpl, GlobalHttpClient}
import com.gu.sfl.persistence.{PersistenceConfig, SavedArticlesPersistenceImpl}
import com.gu.sfl.savedarticles.FetchSavedArticlesImpl

object FetchArticlesConfig {
  private val identityApihostKey = "IdentityApiHost"
  private val stageKey = "Stage"
  private val appKey = "App"

  lazy val identityApiHost = sys.env.getOrElse(identityApihostKey, throw new NullPointerException(identityApihostKey))
  lazy val app = sys.env.getOrElse(appKey, throw new NullPointerException(appKey))
  lazy val stage = sys.env.getOrElse(stageKey, throw new NullPointerException(stageKey))
}

object FetchArticlesLambda extends Logging {

  lazy val cloudWatchImpl = new CloudWatchImpl(app, stage, "fetch", AmazonCloudWatchAsyncClientBuilder.defaultClient())
  lazy val savedArticledController: FetchArticlesController = logOnThrown(
    () => {
      new FetchArticlesController(
        new FetchSavedArticlesImpl(
          new IdentityServiceImpl(IdentityConfig(FetchArticlesConfig.identityApiHost), GlobalHttpClient.defaultHttpClient),
          new SavedArticlesPersistenceImpl(PersistenceConfig(app, stage), cloudWatchImpl)
        )
      )
    }, "Error initialising saved articles controller")
}

class FetchArticlesLambda extends AwsLambda(function = FetchArticlesLambda.savedArticledController, cloudWatchPublisher = FetchArticlesLambda.cloudWatchImpl)
