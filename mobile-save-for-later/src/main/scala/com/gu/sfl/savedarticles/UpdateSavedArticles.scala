package com.gu.sfl.savedarticles

import com.gu.identity.auth.OktaValidationException
import com.gu.sfl.Logging
import com.gu.sfl.exception.{IdentityServiceError, MissingAccessTokenError, OktaOauthValidationError, SaveForLaterError, UserNotFoundError}
import com.gu.sfl.identity.AccessScope.updateSelf
import com.gu.sfl.identity.IdentityService
import com.gu.sfl.lib.{AuthHeaderParser, SavedArticlesMerger}
import com.gu.sfl.model.SavedArticles

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait UpdateSavedArticles {
  def save(headers: Map[String, String], savedArticles: SavedArticles) : Future[Either[SaveForLaterError, SavedArticles]]
}

class UpdateSavedArticlesImpl(identityService: IdentityService, savedArticlesMerger: SavedArticlesMerger)(implicit executionContext: ExecutionContext) extends UpdateSavedArticles with Logging with AuthHeaderParser {

  override def save(headers: Map[String, String], savedArticles: SavedArticles): Future[Either[SaveForLaterError, SavedArticles]] = {
    (for {
      identityHeaders <- getIdentityHeaders(headers)
    } yield {
      val eventualMaybeString = identityService.userFromRequest(identityHeaders, List(updateSelf))
      eventualMaybeString transformWith {
          case Success(Some(userId)) =>
            logger.debug(s"Storing ${savedArticles.numberOfArticles} articles for user $userId")
            Future.successful(savedArticlesMerger.updateWithRetryAndMerge(userId, savedArticles))
          case Success(_) =>
//            logger.debug(s"Could not retrieve a user id for token: ${identityHeaders.auth}")
            Future.successful(Left(new UserNotFoundError("Could not retrieve a user id")))
          case Failure(OktaValidationException(e)) =>
            logger.debug(s"Error retrieving userId from okta oauth token")
            Future.successful(Left(OktaOauthValidationError(e.message, e)))
          case Failure(_) =>
            logger.debug(s"Error retrieving userId for: token: ${identityHeaders.accessToken}")
            Future.successful(Left(new IdentityServiceError("Could not retrieve a user from the id api")))
        }
     }).getOrElse(Future.successful(Left(new MissingAccessTokenError("No access token on request"))))
  }
}
