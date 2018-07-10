package sfl.lambda

import com.gu.AwsIdentity
import com.gu.sfl.Logging
import sfl.controller.FetchArticlesController
import sfl.identity.{IdentityConfig, IdentityServiceImpl}
import sfl.lambda.FetchArticlesLambda.savedArticledController
import sfl.lib.{GlobalHttpClient, SsmConfig}
import sfl.persisitence.{PersistanceConfig, SavedArticlesPersistenceImpl}
import sfl.savedarticles.FetchSavedArticlesImpl

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

class FetchArticlesLambda extends AwsLambda(function = savedArticledController)


