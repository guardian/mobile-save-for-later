package com.gu.sfl.controller

import java.util.concurrent.TimeUnit

import com.gu.sfl.{Logging, Parallelism}
import com.gu.sfl.lambda.{LambdaRequest, LambdaResponse}
import com.gu.sfl.persisitence.SavedArticlesPersistence
import com.gu.sfl.services.IdentityService
import com.gu.sfl.util.StatusCodes
import com.gu.sfl.lib.Jackson.mapper
import com.gu.sfl.savedarticles.FetchSavedArticles

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

object SavedArticlesController {
  val missingUserResponse = LambdaResponse(StatusCodes.badRequest, Some("Could not find a user "))
  val emptyArticlesResponse = LambdaResponse(StatusCodes.ok, Some(mapper.writeValueAsString(SavedArticles(List.empty))))

}

class SavedArticlesController(fetchSavedArticles: FetchSavedArticles) extends Function[LambdaRequest, LambdaResponse] with Logging {
  //TODO pass round ala MAPI
  implicit val executionContext: ExecutionContext = Parallelism.largeGlobalExecutionContext

  override def apply(lambdaRequest: LambdaRequest): LambdaResponse = {

   val futureResponse =  fetchSavedArticles.retrieveSavedArticlesForUser(lambdaRequest).transformWith {
     case Success(Some(savedArticles)) =>
       logger.info(s"Returning found ${savedArticles.articles.size} articles")
       Future { LambdaResponse(StatusCodes.ok, Some(mapper.writeValueAsString(savedArticles)) ) }
     case Success(None) =>
       logger.info("No articles found")
       Future { SavedArticlesController.emptyArticlesResponse  }
     case Failure(_) =>
       logger.info("No saved articles found")
       Future { SavedArticlesController.emptyArticlesResponse }
   }
   //Todo Can move await up
   Await.result(futureResponse, Duration.Inf)
  }

}
