package sfl.controller

import sfl.lambda.{LambdaRequest, LambdaResponse}
import sfl.savedarticles.FetchSavedArticles
import sfl.exception.{IdentityServiceError, MissingAccessTokenError, UserNotFoundError}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class FetchArticlesController(fetchSavedArticles: FetchSavedArticles)(implicit executionContext: ExecutionContext) extends Function[LambdaRequest, Future[LambdaResponse]] with SaveForLaterController  {

  override def defaultErrorMessage: String = "Error fetching articles"

  override def apply(lambdaRequest: LambdaRequest): Future[LambdaResponse] = {

     val futureResponse =  fetchSavedArticles.retrieveForUser(lambdaRequest.headers).map {
       case Right(syncedPrefs) =>
         syncedPrefs.savedArticles.foreach ( sp =>
            logger.debug(s"Returning found: ${sp.articles.size} articles")
         )
         okSyncedPrefsResponse(syncedPrefs)
       case Left(error) =>
         logger.error(s"Error trying to retrieve saved articles: ${error.message}")
         processErrorResponse(error) {
           case e: IdentityServiceError =>  identityErrorResponse
           case e: MissingAccessTokenError => missingAccessTokenResponse
           case e: UserNotFoundError => missingUserResponse
         }
     }
     futureResponse
  }
}
