package com.gu.sfl.controller

import com.gu.sfl.exception.SaveForLaterError
import com.gu.sfl.lambda.LambdaRequest
import com.gu.sfl.lib.Jackson._
import com.gu.sfl.model.{SavedArticle, SavedArticles, SavedArticlesResponse}
import com.gu.sfl.savedarticles.UpdateSavedArticles
import com.gu.sfl.util.StatusCodes
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import java.time.LocalDateTime
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
      val invalidDateTimeString = "2010-01-01 00:00:01" // missing the T separator and trailing Z
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

    /*
     * ==================== TEMPORARY: start of java-default-format fallback tests ====================
     *
     * The 3 tests below were added while we temporarily accept the java-default-format fallback
     * while Android fixes their bug in sending the incorrect format. These tests exist only to
     * document and pin down the temporary fallback behaviour, and MUST fail (or get removed) once
     * that fallback is gone.
     *
     * Task ticket: https://app.asana.com/1/1210045093164357/project/1215309367148854/task/1216728547635263
     */
    "save the articles when the date is in the java default format" in new Setup {
      val javaDefaultDateTimeString = "Fri Jan 01 00:00:01 GMT 2010"
      val json =
        s"""{"version":"1","articles":[{"id":"id/1","shortUrl":"p/1","date": "$javaDefaultDateTimeString","read":false}]}"""

      updateSavedArticles.save(any[Map[String, String]](), any[SavedArticles]()) returns
        Future.successful(Left(mock[SaveForLaterError]))

      Await.result(controller(LambdaRequest(Some(json))), Duration.Inf)

      there was one(updateSavedArticles).save(any[Map[String, String]](), any[SavedArticles]())
    }

    "save the java default format date to the db as the correct parsed LocalDateTime" in new Setup {
      val javaDefaultDateTimeString = "Fri Jan 01 00:00:01 GMT 2010"
      val expectedDateTime = LocalDateTime.of(2010, 1, 1, 0, 0, 1)
      val json =
        s"""{"version":"1","articles":[{"id":"id/1","shortUrl":"p/1","date": "$javaDefaultDateTimeString","read":false}]}"""

      val savedArticlesCaptor = capture[SavedArticles]
      updateSavedArticles.save(any[Map[String, String]](), savedArticlesCaptor) returns
        Future.successful(Left(mock[SaveForLaterError]))

      Await.result(controller(LambdaRequest(Some(json))), Duration.Inf)

      savedArticlesCaptor.value.articles.head.date mustEqual expectedDateTime
    }

    "return the java default format date in the API response as an ISO SavedArticle" in new Setup {
      val javaDefaultDateTimeString = "Fri Jan 01 00:00:01 GMT 2010"
      val expectedArticle = SavedArticle("id/1", "p/1", LocalDateTime.of(2010, 1, 1, 0, 0, 1), read = false)
      val json =
        s"""{"version":"1","articles":[{"id":"id/1","shortUrl":"p/1","date": "$javaDefaultDateTimeString","read":false}]}"""

      updateSavedArticles.save(any[Map[String, String]](), any[SavedArticles]()) answers { (_: Any) match {
        case Array(_, savedArticles: SavedArticles) => Future.successful(Right(savedArticles))
      }}

      val response = Await.result(controller(LambdaRequest(Some(json))), Duration.Inf)
      val parsedResponse = mapper.readValue[SavedArticlesResponse](response.maybeBody.get)

      parsedResponse.savedArticles.articles mustEqual List(expectedArticle)
    }
    /* ==================== TEMPORARY: end of java-default-format fallback tests ==================== */
  }

  trait Setup extends Scope {
    implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global
    val updateSavedArticles = mock[UpdateSavedArticles]
    val controller = new SaveArticlesController(updateSavedArticles)
  }
}
