package com.gu.sfl.controller

import com.gu.sfl.lambda.{LambdaRequest, LambdaResponse}
import com.gu.sfl.savedarticles.FetchSavedArticles
import com.gu.sfl.Logging
import com.gu.sfl.exception.{IdentityServiceException, MissingAccessTokenException, UserNotFoundException}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class FetchArticlesController(fetchSavedArticles: FetchSavedArticles)(implicit executionContext: ExecutionContext) extends Function[LambdaRequest, Future[LambdaResponse]] with SaveForLaterController with Logging {

  override def apply(lambdaRequest: LambdaRequest): Future[LambdaResponse] = {

     val futureResponse =  fetchSavedArticles.retrieveForUser(lambdaRequest.headers).transformWith {
       case Success(Some(syncedPrefs)) =>
         syncedPrefs.savedArticles.foreach ( sp =>
            logger.debug(s"Returning found: ${sp.articles.size} articles")
         )
         Future.successful { okSycedPrefsResponse(syncedPrefs) }
       case Success(None) =>
         logger.debug("No articles found")
         Future.successful { emptyArticlesResponse  }
       case Failure(ex) =>
         logger.debug("Error trying to retrieve saved articles")
         ex match {
           case e: IdentityServiceException => Future.successful( identityErrorResponse )
           case e: MissingAccessTokenException => Future.successful( missingAccessTokenResponse )
           case e: UserNotFoundException => Future.successful(emptyArticlesResponse)
           case _ => Future.successful(serverErrorResponse )
         }
     }
     futureResponse
  }
}
