package com.gu.sfl.controller

import com.gu.sfl.lambda.{LambdaRequest, LambdaResponse}
import com.gu.sfl.savedarticles.FetchSavedArticles
import com.gu.sfl.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


class FetchArticlesController(fetchSavedArticles: FetchSavedArticles)(implicit executionContext: ExecutionContext) extends Function[LambdaRequest, Future[LambdaResponse]] with SaveForLaterController with Logging {

  override def apply(lambdaRequest: LambdaRequest): Future[LambdaResponse] = {

   val futureResponse =  fetchSavedArticles.retrieveForUser(lambdaRequest.headers).transformWith {
     case Success(Some(syncedPrefs)) =>
       syncedPrefs.savedArticles.map( sa =>
          logger.info(s"Returning found ${sa.articles.size} articles")
       )
       Future { okSavedArticlesResponse(syncedPrefs) }
     case Success(None) =>
       logger.info("No articles found")
       Future { emptyArticlesResponse  }
     case Failure(_) =>
       //TODO match and customise error messages
       logger.info("No saved articles found")
       Future { emptyArticlesResponse }
   }
    futureResponse
  }

}
