package com.gu.sfl.savedarticles

import java.time.LocalDateTime

import com.gu.sfl.exception.{IdentityApiRequestError, IdentityServiceError, MissingAccessTokenError, UserNotFoundError}
import com.gu.sfl.identity.{IdentityHeader, IdentityService}
import com.gu.sfl.model.{SavedArticle, SavedArticles, SyncedPrefs}
import com.gu.sfl.persisitence.SavedArticlesPersistence
import org.specs2.matcher.ThrownMessages
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Success


class FetchSavedcArticlesSpec extends Specification with ThrownMessages with Mockito {

  "Fetch articles without auth headers does ot call identity api" in new Setup {
     fetchSavedArticlesImpl.retrieveForUser(Map.empty)
     there were no(identityService).userFromRequest(any[IdentityHeader]())
  }

  "Fetch articles without auth headers does not attempt to retrieve persisitin articles" in new Setup {
    fetchSavedArticlesImpl.retrieveForUser(Map.empty)
    there were no(savedArticlesPersistence).read(any[String]())
  }

  "fetching articles without auth headers results in the correct exception" in new Setup {
    val invalidResult = Await.ready(fetchSavedArticlesImpl.retrieveForUser(Map.empty), Duration.Inf).value.get

    invalidResult map {
      case Left(_) => fail("No missing auth exception thrown")
      case Right(ex) => ex mustEqual(MissingAccessTokenError("No access token on request"))
    }
  }

  "fetching saved articles correctly sends the auth headers to the identity service" in new SetupWithUserId {
    fetchSavedArticlesImpl.retrieveForUser(requestHeaders)
    there was one (identityService).userFromRequest(argThat(===(identityHeaders)))
  }

  "when the identity service provides a user id we retrieve the articles from the persistence layers" in new SetupWithUserId {
    val res = fetchSavedArticlesImpl.retrieveForUser(requestHeaders)
    Await.result(res, Duration.Inf)
    there was one (savedArticlesPersistence).read(argThat(===(userId)))
  }

  "when the identity provides a user id the users saved articles are returned" in new SetupWithUserId {
    savedArticlesPersistence.read(argThat(===(userId))) returns(Success(Some(savedArticles)))
    val savedArticlesFuture = fetchSavedArticlesImpl.retrieveForUser(requestHeaders)
    Await.result(savedArticlesFuture, Duration.Inf) mustEqual(Right(SyncedPrefs(userId, Some(savedArticles))))
  }

  "when there are duplicates articles in the returned articles list they are removed" in new SetupWithUserId {
    savedArticlesPersistence.read((argThat(===(userId)))) returns(Success(Some(savedArticleWithDupes)))
    val articlesResult = Await.ready(fetchSavedArticlesImpl.retrieveForUser(Map.empty), Duration.Inf).value.get
    articlesResult map {
      case Left(_) => fail("Should not fail ever")
      case Right(fetchedArticles) => fetchedArticles.savedArticles.map(_.articles).getOrElse(List.empty).map(_.id) mustEqual(articleIds)
    }
  }

  "when the user has no articles then the response contains an empty list" in new SetupWithUserId {
    val emptyArticles = SavedArticles("123454", List.empty)
    savedArticlesPersistence.read(argThat(===(userId))) returns(Success(Some(emptyArticles)))
    val savedArticlesFuture = fetchSavedArticlesImpl.retrieveForUser(requestHeaders)
    Await.result(savedArticlesFuture, Duration.Inf) mustEqual(Right(SyncedPrefs(userId, Some(emptyArticles))))
  }

  "when the user has never saved any articles the response contains an empty list" in new SetupWithUserId {
     val emptyArticles = SavedArticles("1", List.empty)
     savedArticlesPersistence.read(argThat(===(userId))).returns(Success(None))
     val savedArticlesFuture = fetchSavedArticlesImpl.retrieveForUser(requestHeaders)
     Await.result(savedArticlesFuture, Duration.Inf) mustEqual(Right(SyncedPrefs(userId, Some(emptyArticles))))
  }


  "when the identity api does not return a user id then no attemp is made to retrieve persisted articles" in new SeupWithoutUserId  {
    fetchSavedArticlesImpl.retrieveForUser(requestHeaders)
    there were no (savedArticlesPersistence).read(any[String]())
  }

  "when the identity service does not provide a user id the correct error is returned" in new SeupWithoutUserId {
    val futureFetchException = Await.ready(fetchSavedArticlesImpl.retrieveForUser(requestHeaders), Duration.Inf).value.get
    futureFetchException map {
      case Right(_) => fail("No missing user errot")
      case Left(ex) => ex mustEqual(new UserNotFoundError("Could not retrieve a user id"))
    }
  }

  "when the identity service errors the correct error is returned" in new Setup {
    identityService.userFromRequest(any[IdentityHeader]()) returns(Future.failed(IdentityApiRequestError("Did not get identiy api response")))
    val futureFetchException = Await.ready(fetchSavedArticlesImpl.retrieveForUser(requestHeaders), Duration.Inf)
    futureFetchException map {
      case Right(_) => fail("No missing user errot")
      case Left(ex) => ex mustEqual(new IdentityServiceError("Could not get a response from the id api"))
    }
  }

  trait SeupWithoutUserId extends Setup {
    identityService.userFromRequest(any[IdentityHeader]()) returns(Future.successful(None))
  }

  trait SetupWithUserId extends Setup {
    identityService.userFromRequest(any[IdentityHeader]()) returns(Future.successful(Some(userId)))
  }


  trait Setup extends Scope {
    protected val userId = "1234"
    protected val version = "123432"

    val savedArticles = SavedArticles(version, List(
      SavedArticle("id/1", "p/1", LocalDateTime.of(2018, 1, 16, 16, 30), read = true),
      SavedArticle("id/2", "p/2", LocalDateTime.of(2018, 2, 17, 17, 30), read = false),
      SavedArticle("id/3", "p/3", LocalDateTime.of(2018, 3, 18, 18, 45), read = true)
    ))

    lazy val articleIds = savedArticles.articles.map(_.id)

    lazy val savedArticleWithDupes = savedArticles.copy(
      articles = SavedArticle("id/1", "p/1", LocalDateTime.of(2014, 1, 16, 7, 30), read = true) :: SavedArticle("id/2", "p/2", LocalDateTime.of(2015, 2, 17, 17, 30), read = false) :: savedArticles.articles)




    val identityService = mock[IdentityService]
    val savedArticlesPersistence = mock[SavedArticlesPersistence]

    val fetchSavedArticlesImpl = new FetchSavedArticlesImpl(identityService, savedArticlesPersistence)
    val requestHeaders = Map("authorization" -> "some_auth", "X-GU-ID-Client-Access-Token" -> "Bearer application_token")
    val identityHeaders = IdentityHeader(auth = "some_auth", accessToken = "Bearer application_token")


  }

}
