package com.gu.sfl.savedarticles

import com.gu.sfl.exception._
import com.gu.sfl.identity.IdentityService
import com.gu.sfl.lib.AuthHeaderParser
import com.gu.sfl.model._
import com.gu.sfl.persisitence.SavedArticlesPersistence
import com.gu.sfl.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait FetchSavedArticles {
    def retrieveForUser(headers: Map[String, String]) : Future[Either[SaveForLaterError, SyncedPrefs]]
}

class FetchSavedArticlesImpl(identityService: IdentityService, savedArticlesPersistence: SavedArticlesPersistence)(implicit executionContext: ExecutionContext) extends FetchSavedArticles with Logging with AuthHeaderParser{

  private def wrapSavedArticles(userId: String, maybeSavedArticles: Try[Option[SavedArticles]]) : Either[SaveForLaterError, SyncedPrefs] = maybeSavedArticles match {
    case Success(Some(articles)) => Right(SyncedPrefs(userId, Some(articles)))
    case Success(_) => Right(SyncedPrefs(userId, Some(SavedArticles.empty)))
    case _ => Left(RetrieveSavedArticlesError("Could not update articles"))
  }

  override def retrieveForUser(headers: Map[String, String]): Future[Either[SaveForLaterError, SyncedPrefs]] = {
    (for {
      identityHeaders <- getIdentityHeaders(headers)
    } yield {
      identityService.userFromRequest(identityHeaders).transformWith{
        case Success(Some(userId)) =>
          logger.info(s"Got user id ${userId} from identity")
          Future.successful(wrapSavedArticles(userId, savedArticlesPersistence.read(userId)))
        case Success(_) =>
          logger.debug(s"no user found for AccessToken ${identityHeaders.accessToken}")
          Future.successful(Left(new UserNotFoundException("Could not retrieve a user id")))
        case Failure(_) =>
          logger.debug(s"Error retrieving userId for: token: ${identityHeaders.accessToken}")
          Future.successful(Left(new IdentityServiceException("Could not get a response from the id api")))
      }
    }).getOrElse{
      logger.debug(s"Could not retrieve identity headers")
      Future.successful(Left(MissingAccessTokenException("No access token on request")))
    }
  }
}
