package com.gu.sfl.savedarticles

import com.gu.sfl.exception.{IdentityServiceException, MissingAccessTokenException, UserNotFoundException}
import com.gu.sfl.identity.IdentityService
import com.gu.sfl.lib.{AuthHeaderParser, SavedArticlesMerger}
import com.gu.sfl.model.{SavedArticles, SyncedPrefs}
import com.gu.sfl.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait UpdateSavedArticles {
  def save(headers: Map[String, String], savedArticles: SavedArticles) : Future[Option[SyncedPrefs]]
}

class UpdateSavedArticlesImpl(identityService: IdentityService, savedArticlesMerger: SavedArticlesMerger)(implicit executionContext: ExecutionContext) extends UpdateSavedArticles with Logging with AuthHeaderParser {

  override def save(headers: Map[String, String], savedArticles: SavedArticles): Future[Option[SyncedPrefs]] = {
    (for {
      identityHeaders <- getIdentityHeaders(headers)
    } yield {
        identityService.userFromRequest(identityHeaders) transformWith {
          case Success(Some(userId)) =>
            logger.debug(s"Attempting to save articles fo user: $userId")
            Future.fromTry(savedArticlesMerger.updateWithRetryAndMerge(userId, savedArticles))
          case Success(_) =>
            logger.debug(s"Could not retrieve a user id for token: ${identityHeaders.auth}")
            Future.failed (new UserNotFoundException("Could not retrieve a user id"))
          case Failure(_) =>
            logger.debug(s"Error retrieving userId for: token: ${identityHeaders.accessToken}")
            Future.failed (new IdentityServiceException("Could not retrieve a user from the id api"))
        }
     }).getOrElse(Future.failed(new MissingAccessTokenException("No access token on request")))
  }
}
