package com.gu.sfl.savedarticles

import java.time.LocalDateTime

import com.gu.sfl.Parallelism
import com.gu.sfl.controller.{SavedArticle, SavedArticles}
import com.gu.sfl.exception.{IdentityApiRequestError, MissingAccessTokenException, UserNotFoundException}
import com.gu.sfl.identity.{IdentityHeaders, IdentityService}
import com.gu.sfl.lib.SavedArticlesMerger
import org.specs2.matcher.ThrownMessages
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}
import scala.concurrent.{Await, ExecutionContext, Future}

class UpdateSavedArticlesSpec extends Specification with ThrownMessages with Mockito {

  implicit val executionContext: ExecutionContext = Parallelism.largeGlobalExecutionContext

  "update articles without auth header should not call the id api" in new Setup {
     updateSavedArticles.saveSavedArticles(Map.empty, savedArticles)
     there were no(identityService).userFromRequest(any[IdentityHeaders]())
  }

  "update articles without auth header should not try to merge articles" in new Setup {
     updateSavedArticles.saveSavedArticles(Map.empty, savedArticles)
     there were no(articlesMerger).updateWithRetryAndMerge(any[String](), any[SavedArticles]())
  }

  "update articles without auth header fail with the correct exception" in new Setup {

     val updateResponse = Await.ready(updateSavedArticles.saveSavedArticles(Map.empty, savedArticles), Duration.Inf).value.get

    updateResponse match {
      case Success(_) => fail("No IOxception thrown")
      case Failure(e) => e mustEqual(MissingAccessTokenException("No access token on request"))
    }
  }

  "attempting to update the articles without a user id to does not merge the new article list" in new SetupWithNoUserId  {
    updateSavedArticles.saveSavedArticles(requestHeaders, savedArticles)
    there were no(articlesMerger).updateWithRetryAndMerge(any[String](), any[SavedArticles]())
  }

  "attempting to update the articles without a userid fails the correct exception" in new SetupWithNoUserId  {
    val updateResponse = Await.ready(updateSavedArticles.saveSavedArticles(requestHeaders, savedArticles), Duration.Inf).value.get

    updateResponse match {
      case Success(_) => fail("No IOxception thrown")
      case Failure(e) => e mustEqual(UserNotFoundException("Could not retrieve a user id"))
    }
  }

  "attempting to update the articles does not merge them if the identity request fails" in new SetUpWithUserId {
    identityService.userFromRequest(any[IdentityHeaders]()) returns (Future.failed(IdentityApiRequestError("Did not get identiy api response")))
    updateSavedArticles.saveSavedArticles(requestHeaders, savedArticles)
    there were no(articlesMerger).updateWithRetryAndMerge(any[String](), any[SavedArticles]())
  }

  "attempting to update the articles fails with correct exception them if the identity request fails" in new SetUpWithUserId {
    identityService.userFromRequest(any[IdentityHeaders]()) returns (Future.failed(IdentityApiRequestError("Did not get identiy api response")))
    val updateResponse = Await.ready(updateSavedArticles.saveSavedArticles(requestHeaders, savedArticles), Duration.Inf).value.get

    updateResponse match {
      case Success(_) => fail("No IOxception thrown")
      case Failure(e) => e mustEqual(UserNotFoundException("Could not retrieve a user id"))
    }
  }

  "updating articles passes the correct header values to send to identity api" in new SetUpWithUserId  {
    updateSavedArticles.saveSavedArticles(requestHeaders, savedArticles)
    there was one(identityService).userFromRequest(argThat(===(identityHeaders)))
  }

  "when the identity service returns a user id the articles are merged" in new SetUpWithUserId {
    updateSavedArticles.saveSavedArticles(requestHeaders, savedArticles)
    there was one(articlesMerger).updateWithRetryAndMerge(argThat(===(userId)), argThat(===(savedArticles)))
  }

  "when the identity service returns a user the updated articles are returned" in new SetUpWithUserId  {
    articlesMerger.updateWithRetryAndMerge(any[String](), any[SavedArticles]()) returns (Success(Some(updatedSavedArticles)))
    val updateResponse = updateSavedArticles.saveSavedArticles(requestHeaders, savedArticles)
    Await.result(updateResponse, Duration.Inf)  mustEqual(Some(updatedSavedArticles))
  }


  trait SetupWithNoUserId extends Setup {
    identityService.userFromRequest(any[IdentityHeaders]()) returns (Future.successful(None))
  }

  trait SetUpWithUserId extends Setup {
    identityService.userFromRequest(any[IdentityHeaders]()) returns (Future.successful(Some(userId)))
  }

  trait Setup extends Scope {

    val( articleOne, articleTwo, articleThree) = (
      SavedArticle("id/1", "p/1", LocalDateTime.of(2018, 1, 16, 16, 30), read = true),
      SavedArticle("id/2", "p/2", LocalDateTime.of(2018, 2, 17, 17, 30), read = false),
      SavedArticle("id/3", "p/3", LocalDateTime.of(2018, 3, 18, 18, 45), read = true)
    )

    protected val userId = "1234"

    val savedArticles = SavedArticles(userId, List(articleOne, articleTwo))
    val updatedSavedArticles = SavedArticles(userId, List(articleOne, articleTwo, articleThree))

    val identityService = mock[IdentityService]
    val articlesMerger = mock[SavedArticlesMerger]
    val updateSavedArticles = new UpdateSavedArticlesImpl(identityService, articlesMerger)
    val requestHeaders = Map("Authorization" -> "some_auth")
    val identityHeaders = IdentityHeaders(auth = "some_auth", accessToken = "Bearer application_token")
  }

}
