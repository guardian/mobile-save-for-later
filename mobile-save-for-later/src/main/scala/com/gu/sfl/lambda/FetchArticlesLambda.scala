package com.gu.sfl.lambda

import com.gu.identity.auth.{OktaAudience, OktaIssuerUrl, OktaLocalAccessTokenValidator, OktaTokenValidationConfig}
import com.gu.sfl.Logging
import com.gu.sfl.controller.FetchArticlesController
import com.gu.sfl.identity.IdentityServiceImpl
import com.gu.sfl.lambda.AwsLambda.readEnvKey
import com.gu.sfl.lambda.FetchArticlesConfig.{app, stage}
import com.gu.sfl.lib.Parallelism.largeGlobalExecutionContext
import com.gu.sfl.persistence.{PersistenceConfig, SavedArticlesPersistenceImpl}
import com.gu.sfl.savedarticles.FetchSavedArticlesImpl

object FetchArticlesConfig {
  lazy val app = readEnvKey("App")
  lazy val stage = readEnvKey("Stage")
  lazy val identityOktaIssuerUrl = readEnvKey("IdentityOktaIssuerUrl")
  lazy val identityOktaAudience = readEnvKey("IdentityOktaAudience")
}

object FetchArticlesLambda extends Logging {

  lazy val fetchArticlesController: FetchArticlesController = logOnThrown(
    () => {
      new FetchArticlesController(
        new FetchSavedArticlesImpl(
          new IdentityServiceImpl(
            OktaLocalAccessTokenValidator.fromConfig(
              OktaTokenValidationConfig(
                OktaIssuerUrl(FetchArticlesConfig.identityOktaIssuerUrl),
                Some(OktaAudience(FetchArticlesConfig.identityOktaAudience)),
                None
              )
            ).get
          ),
          new SavedArticlesPersistenceImpl(PersistenceConfig(app, stage))
        )
      )
    }, "Error initialising fetch articles controller")
}

class FetchArticlesLambda extends AwsLambda(function = FetchArticlesLambda.fetchArticlesController)
