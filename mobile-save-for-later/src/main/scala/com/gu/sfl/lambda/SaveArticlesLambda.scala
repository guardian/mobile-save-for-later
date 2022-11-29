package com.gu.sfl.lambda
import com.gu.identity.auth.{OktaLocalValidator, OktaTokenValidationConfig}
import com.gu.sfl.Logging
import com.gu.sfl.controller.SaveArticlesController
import com.gu.sfl.identity.{IdentityConfig, IdentityServiceImpl}
import com.gu.sfl.lambda.AwsLambda.readEnvKey
import com.gu.sfl.lambda.SaveArticlesConfig.{app, stage}
import com.gu.sfl.lib.Parallelism.largeGlobalExecutionContext
import com.gu.sfl.lib.{GlobalHttpClient, SavedArticlesMergerConfig, SavedArticlesMergerImpl}
import com.gu.sfl.persistence.{PersistenceConfig, SavedArticlesPersistenceImpl}
import com.gu.sfl.savedarticles.UpdateSavedArticlesImpl
object SaveArticlesConfig {

  lazy val identityApiHost: String = readEnvKey("IdentityApiHost")
  lazy val app: String = readEnvKey("App")
  lazy val stage: String = readEnvKey("Stage")
  lazy val savedArticleLimit: Int = readEnvKey("SavedArticleLimit").toInt
  lazy val identityOktaIssuerUrl = readEnvKey("IdentityOktaIssuerUrl")
  lazy val identityOktaAudience = readEnvKey("IdentityOktaAudience")
}
object SaveArticlesLambda extends Logging {
  lazy val saveForLaterController: SaveArticlesController = logOnThrown(
    () => {
          new SaveArticlesController(
            new UpdateSavedArticlesImpl(
              new IdentityServiceImpl(IdentityConfig(SaveArticlesConfig.identityApiHost), GlobalHttpClient.defaultHttpClient, OktaLocalValidator.fromConfig(OktaTokenValidationConfig(SaveArticlesConfig.identityOktaIssuerUrl, SaveArticlesConfig.identityOktaAudience))),
              new SavedArticlesMergerImpl(SavedArticlesMergerConfig(SaveArticlesConfig.savedArticleLimit),
                new SavedArticlesPersistenceImpl(PersistenceConfig(app, stage))
              )
            )
          )
    }, "Error initialising save for later controller")
}


class SaveArticlesLambda extends AwsLambda(SaveArticlesLambda.saveForLaterController)
