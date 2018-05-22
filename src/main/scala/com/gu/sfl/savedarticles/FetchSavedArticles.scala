package com.gu.sfl.savedarticles

import com.gu.sfl.exception.{MissingAccessTokenException, RetrieveSavedArticlesError, UserNotFoundException}
import com.gu.sfl.identity.IdentityService
import com.gu.sfl.lib.AuthHeaderParser
import com.gu.sfl.model._
import com.gu.sfl.persisitence.SavedArticlesPersistence
import com.gu.sfl.{Logging, Parallelism}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait FetchSavedArticles {
    def retrieveSavedArticlesForUser(headers: Map[String, String]) : Future[Option[SyncedPrefs]]
}

class FetchSavedArticlesImpl(identityService: IdentityService, savedArticlesPersistence: SavedArticlesPersistence) extends FetchSavedArticles with Logging with AuthHeaderParser{

  implicit val executionContext: ExecutionContext = Parallelism.largeGlobalExecutionContext

  private def wrapSavedArticles(userId: String, maybeSavedArticles: Try[Option[SavedArticles]]) : Try[Option[SyncedPrefs]] = maybeSavedArticles match {
    case Success(Some(articles)) => Success(Some(SyncedPrefs(userId, Some(articles))))
    case _ => Failure(RetrieveSavedArticlesError("Could not update articles"))
  }

  //TODO rename
  override def retrieveSavedArticlesForUser(headers: Map[String, String]): Future[Option[SyncedPrefs]] = {
    for{(key, value) <- headers} logger.info(s"Header name: ${key} value: ${value}")

    (for {
      identityHeaders <- getIdentityHeaders(headers)
    } yield {
      identityService.userFromRequest(identityHeaders).transformWith{
        case Success(Some(userId)) =>
          logger.info(s"Got user id ${userId} from identity")
          Future.fromTry(wrapSavedArticles(userId, savedArticlesPersistence.read(userId)))
        case Success(_) =>
          logger.info(s"no user found for AccessToken ${identityHeaders.accessToken}")
          Future.failed(new UserNotFoundException("Could not retrieve a user id"))
        case Failure(_) =>
          logger.info(s"Error retrieving userId for: token: ${identityHeaders.accessToken}")
          Future.failed(new UserNotFoundException("Could not retrieve a user id"))
      }
    }).getOrElse{
      logger.info(s"Could not retrieve identity headers")
      Future.failed(MissingAccessTokenException("No access token on request"))
    }
  }
}
