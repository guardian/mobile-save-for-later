package com.gu.sfl.savedarticles

import java.time.LocalDateTime

import com.gu.sfl.exception.{IdentityApiRequestError, IdentityServiceError, MissingAccessTokenError, UserNotFoundError}
import com.gu.sfl.identity.{IdentityHeader, IdentityService}
import com.gu.sfl.lib.SavedArticlesMerger
import com.gu.sfl.model.{SavedArticle, SavedArticles}
import org.specs2.matcher.ThrownMessages
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import com.gu.sfl.lib.Parallelism.largeGlobalExecutionContext
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


class UpdateSavedArticlesSpec extends Specification with ThrownMessages with Mockito {

  "update articles without auth header should not call the id api" in new Setup {
     updateSavedArticles.save(Map.empty, savedArticles)
     there were no(identityService).userFromRequest(any[IdentityHeader]())
  }

  "update articles without auth header should not try to merge articles" in new Setup {
     updateSavedArticles.save(Map.empty, savedArticles)
     there were no(articlesMerger).updateWithRetryAndMerge(any[String](), any[SavedArticles]())
  }

  "update articles without auth header fail with the correct exception" in new Setup {

     val updateResponse = Await.ready(updateSavedArticles.save(Map.empty, savedArticles), Duration.Inf).value.get

    updateResponse map {
      case Left(_) => fail("No IOxception thrown")
      case Right(e) => e mustEqual(MissingAccessTokenError("No access token on request"))
    }
  }

  "attempting to update the articles without a user id to does not merge the new article list" in new SetupWithNoUserId  {
    updateSavedArticles.save(requestHeaders, savedArticles)
    there were no(articlesMerger).updateWithRetryAndMerge(any[String](), any[SavedArticles]())
  }

  "attempting to update the articles without a userid fails the correct exception" in new SetupWithNoUserId  {
    val updateResponse = Await.ready(updateSavedArticles.save(requestHeaders, savedArticles), Duration.Inf).value.get

    updateResponse map {
      case Left(_) => fail("No IOxception thrown")
      case Right(e) => e mustEqual(UserNotFoundError("Could not retrieve a user id"))
    }
  }

  "attempting to update the articles does not merge them if the identity request fails" in new SetUpWithUserId {
    identityService.userFromRequest(any[IdentityHeader]()) returns (Future.failed(IdentityApiRequestError("Did not get identiy api response")))
    updateSavedArticles.save(requestHeaders, savedArticles)
    there were no(articlesMerger).updateWithRetryAndMerge(any[String](), any[SavedArticles]())
  }

  "attempting to update the articles fails with correct exception them if the identity request fails" in new SetUpWithUserId {
    identityService.userFromRequest(any[IdentityHeader]()) returns (Future.failed(IdentityApiRequestError("Did not get identiy api response")))
    val updateResponse = Await.ready(updateSavedArticles.save(requestHeaders, savedArticles), Duration.Inf).value.get

    updateResponse map {
      case Right(_) => fail("No IOxception thrown")
      case Left(e) => e mustEqual(IdentityServiceError("Could not retrieve a user from the id api"))
    }
  }

  "updating articles passes the correct header values to send to identity api" in new SetUpWithUserId  {
    updateSavedArticles.save(requestHeaders, savedArticles)
    there was one(identityService).userFromRequest(argThat(===(identityHeaders)))
  }

  "when the identity service returns a user id the articles are merged" in new SetUpWithUserId {
    val updateResponse = updateSavedArticles.save(requestHeaders, savedArticles)
    Await.result(updateResponse, Duration.Inf)
    there was one(articlesMerger).updateWithRetryAndMerge(argThat(===(userId)), argThat(===(savedArticles)))
  }

  "when the identity service returns a user the updated articles are returned" in new SetUpWithUserId  {
    articlesMerger.updateWithRetryAndMerge(any[String](), any[SavedArticles]()) returns (Right(updatedSavedArticles))
    val updateResponse = updateSavedArticles.save(requestHeaders, savedArticles)
    Await.result(updateResponse, Duration.Inf)  mustEqual(Right(updatedSavedArticles))
  }

  trait SetupWithNoUserId extends Setup {
    identityService.userFromRequest(any[IdentityHeader]()) returns (Future.successful(None))
  }

  trait SetUpWithUserId extends Setup {
    identityService.userFromRequest(any[IdentityHeader]()) returns (Future.successful(Some(userId)))
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
    val requestHeaders = Map("authorization" -> "some_auth")
    val identityHeaders = IdentityHeader(auth = "some_auth", accessToken = "Bearer application_token")
  }

}
