package com.gu.sfl.savedarticles

import com.gu.sfl.{Logging, Parallelism}
import com.gu.sfl.controller.{SavedArticles, SyncedPrefs}
import com.gu.sfl.exception.{MissingAccessTokenException, UserNotFoundException}
import com.gu.sfl.identity.{IdentityHeaders, IdentityService}
import com.gu.sfl.lambda.LambdaRequest
import com.gu.sfl.lib.Jackson.mapper
import com.gu.sfl.lib.{AuthHeaderParser, SavedArticlesMerger}
import com.gu.sfl.persisitence.{SavedArticlesPersistence, SavedArticlesPersistenceImpl}
import com.gu.sfl.util.HeaderNames._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

//TODO rename
trait UpdateSavedArticles {
  //TODO - rename this too!
  def saveSavedArticles(headers: Map[String, String], savedArticles: SavedArticles) : Future[Option[SyncedPrefs]]
}

class UpdateSavedArticlesImpl(identityService: IdentityService, savedArticlesMerger: SavedArticlesMerger) extends UpdateSavedArticles with Logging with AuthHeaderParser {

  implicit val executionContext: ExecutionContext = Parallelism.largeGlobalExecutionContext

  override def saveSavedArticles(headers: Map[String, String], savedArticles: SavedArticles): Future[Option[SyncedPrefs]] = {
    (for {
      identityHeaders <- getIdentityHeaders(headers)
    } yield {
        identityService.userFromRequest(identityHeaders) transformWith {
          case Success(Some(userId)) =>
            logger.info(s"Attempting to save articles fo user: $userId")
            Future.fromTry(savedArticlesMerger.updateWithRetryAndMerge(userId, savedArticles))
          case Success(_) =>
            logger.info(s"Could not retrieve a user id for token: ${identityHeaders.auth}")
            Future.failed (new UserNotFoundException("Could not retrieve a user id"))
          case Failure(_) =>
            logger.info(s"Error retrieving userId for: token: ${identityHeaders.accessToken}")
            Future.failed (new UserNotFoundException("Could not retrieve a user id"))
        }
     }).getOrElse(Future.failed(new MissingAccessTokenException("No access token on request")))
  }
}
