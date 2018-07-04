package com.gu.sfl.controller

import com.gu.sfl.lambda.{LambdaRequest, LambdaResponse}
import com.gu.sfl.savedarticles.FetchSavedArticles
import com.gu.sfl.Logging
import com.gu.sfl.exception.{IdentityServiceException, MissingAccessTokenException, UserNotFoundException}

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
       case Left(ex) =>
         logger.error(s"Error trying to retrieve saved articles: ${ex.message}")
         processErrorResponse(ex) {
           case e: IdentityServiceException =>  identityErrorResponse
           case e: MissingAccessTokenException => missingAccessTokenResponse
           case e: UserNotFoundException => missingUserResponse
         }
     }
     futureResponse
  }
}
