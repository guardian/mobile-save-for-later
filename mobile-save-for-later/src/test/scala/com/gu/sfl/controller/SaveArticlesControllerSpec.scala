package com.gu.sfl.controller

import com.gu.sfl.exception.SaveForLaterError
import com.gu.sfl.lambda.LambdaRequest
import com.gu.sfl.model.SavedArticles
import com.gu.sfl.savedarticles.UpdateSavedArticles
import com.gu.sfl.util.StatusCodes
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class SaveArticlesControllerSpec extends Specification with Mockito {

  "SaveArticlesController" should {
    "save the articles when the date is in the expected format" in new Setup {
      val validDateTimeString = "2026-07-16T10:15:30Z"
      val json = s"""{"version":"1","articles":[{"id":"id/1","shortUrl":"p/1","date": "$validDateTimeString","read":false}]}"""
      updateSavedArticles.save(any[Map[String, String]](), any[SavedArticles]()) returns Future.successful(Left(mock[SaveForLaterError]))

      Await.result(controller(LambdaRequest(Some(json))), Duration.Inf)

      there was one(updateSavedArticles).save(any[Map[String, String]](), any[SavedArticles]())
    }

    "return a 400 when the date is not in the expected format" in new Setup {
      val invalidDateTimeString = "Fri Jan 01 08:00:01 GMT+08:00 2010"
      val json = s"""{"version":"1","articles":[{"id":"id/1","shortUrl":"p/1","date": "$invalidDateTimeString","read":false}]}"""
      val response = Await.result(controller(LambdaRequest(Some(json))), Duration.Inf)

      response.statusCode mustEqual StatusCodes.badRequest
      there were no(updateSavedArticles).save(any[Map[String, String]](), any[SavedArticles]())
    }

    "return a 400 when the request body is not valid json" in new Setup {
      val response = Await.result(controller(LambdaRequest(Some("not json"))), Duration.Inf)

      response.statusCode mustEqual StatusCodes.badRequest
    }

    "return a 400 when there is no request body" in new Setup {
      val response = Await.result(controller(LambdaRequest(None)), Duration.Inf)

      response.statusCode mustEqual StatusCodes.badRequest
    }
  }

  trait Setup extends Scope {
    implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global
    val updateSavedArticles = mock[UpdateSavedArticles]
    val controller = new SaveArticlesController(updateSavedArticles)
  }
}
