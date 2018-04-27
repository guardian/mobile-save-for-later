package com.gu.sfl.savedarticles

import com.gu.sfl.{Logging, Parallelism}
import com.gu.sfl.controller.SavedArticles
import com.gu.sfl.exception.{MissingAccessToken, UserNotFoundException}
import com.gu.sfl.lambda.LambdaRequest
import com.gu.sfl.lib.Jackson.mapper
import com.gu.sfl.persisitence.{SavedArticlesPersistence, SavedArticlesPersistenceImpl}
import com.gu.sfl.services.{IdentityHeaders, IdentityService}
import com.gu.sfl.util.HeaderNames._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

//TODO rename
trait UpdateSavedArticles {
  //TODO - rename this too!
  def saveSavedArticles(headers: Map[String, String], savedArticles: SavedArticles) : Future[Option[SavedArticles]]
}

class UpdateSavedArticlesImpl(identityService: IdentityService, savedArticlesPersistence: SavedArticlesPersistence) extends UpdateSavedArticles with Logging {

  implicit val executionContext: ExecutionContext = Parallelism.largeGlobalExecutionContext

  //PullUp
  private def getIdentityHeaders(headers: Map[String, String]) : Option[IdentityHeaders] = for {
     auth <- headers.get(Identity.auth)
  } yield IdentityHeaders(auth = auth)

  override def saveSavedArticles(headers: Map[String, String], savedArticles: SavedArticles): Future[Option[SavedArticles]] = {
    (for {
      identityHeaders <- getIdentityHeaders(headers)
    } yield {
        identityService.userFromRequest(identityHeaders) transformWith {
          case Success(Some(userId)) =>
            logger.info(s"Attempting to save articles fo user: $userId")
            Future.fromTry(savedArticlesPersistence.write(userId, savedArticles))
          case _ =>
            Future.failed (new UserNotFoundException("Could not retrieve a user id"))
        }
     }).getOrElse(Future.failed(new MissingAccessToken("No access token on request")))
  }
}
