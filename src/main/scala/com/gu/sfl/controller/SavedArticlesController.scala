package com.gu.sfl.controller

import java.util.concurrent.TimeUnit

import com.gu.sfl.identity.IdentityService
import com.gu.sfl.{Logging, Parallelism}
import com.gu.sfl.lambda.{LambdaRequest, LambdaResponse}
import com.gu.sfl.persisitence.SavedArticlesPersistence
import com.gu.sfl.util.StatusCodes
import com.gu.sfl.lib.Jackson.mapper
import com.gu.sfl.savedarticles.FetchSavedArticles

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}


class SavedArticlesController(fetchSavedArticles: FetchSavedArticles) extends Function[LambdaRequest, Future[LambdaResponse]] with SaveForLaterController with Logging {

  implicit val executionContext: ExecutionContext = Parallelism.largeGlobalExecutionContext

  override def apply(lambdaRequest: LambdaRequest): Future[LambdaResponse] = {

   val futureResponse =  fetchSavedArticles.retrieveSavedArticlesForUser(lambdaRequest.headers).transformWith {
     case Success(Some(syncedPrefs)) =>
       logger.info(s"Returning found ${syncedPrefs.size} articles")
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
