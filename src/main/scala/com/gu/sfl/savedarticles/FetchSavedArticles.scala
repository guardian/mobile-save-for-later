package com.gu.sfl.savedarticles

import com.gu.sfl.{Logging, Parallelism}
import com.gu.sfl.controller.SavedArticles
import com.gu.sfl.exception.{MissingAccessTokenException, UserNotFoundException}
import com.gu.sfl.identity.{IdentityHeaders, IdentityService}
import com.gu.sfl.persisitence.SavedArticlesPersistence
import com.gu.sfl.util.HeaderNames._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait FetchSavedArticles {
    def retrieveSavedArticlesForUser(headers: Map[String, String]) : Future[Option[SavedArticles]]
}

class FetchSavedArticlesImpl(identityService: IdentityService, savedArticlesPersistence: SavedArticlesPersistence) extends FetchSavedArticles with Logging{

  implicit val executionContext: ExecutionContext = Parallelism.largeGlobalExecutionContext

  private def getIdentityHeaders(headers: Map[String, String]) : Option[IdentityHeaders] = for {
    auth <- headers.get(Identity.auth)
    token <- headers.get(Identity.accessToken)
  } yield IdentityHeaders(auth = auth, accessToken = token)

  //TODO rename
  override def retrieveSavedArticlesForUser(headers: Map[String, String]): Future[Option[SavedArticles]] = {
    for{(key, value) <- headers} logger.info(s"Header name: ${key} value: ${value}")

    (for {
      identityHeaders <- getIdentityHeaders(headers)
    } yield {
      identityService.userFromRequest(identityHeaders).transformWith{
        case Success(Some(userId)) =>
          logger.info(s"Got user id ${userId} from identity")
          Future.fromTry(savedArticlesPersistence.read(userId))
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
