package com.gu.sfl.savedarticles

import com.gu.sfl.{Logging, Parallelism}
import com.gu.sfl.controller.SavedArticles
import com.gu.sfl.lambda.LambdaRequest
import com.gu.sfl.persisitence.SavedArticlesPersistence
import com.gu.sfl.services.{IdentityHeaders, IdentityService}
import com.gu.sfl.util.HeaderNames._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait FetchSavedArticles {
    def retrieveSavedArticlesForUser(lambdaRequest: LambdaRequest) : Future[Option[SavedArticles]]
}

class FetchSavedArticlesImpl(identityService: IdentityService, savedArticlesPersistence: SavedArticlesPersistence) extends FetchSavedArticles with Logging{

  implicit val executionContext: ExecutionContext = Parallelism.largeGlobalExecutionContext

  private def getIdentityHeaders(headers: Map[String, String]) : Option[IdentityHeaders] = for {
    auth <- headers.get(Identity.auth)
    token <- headers.get(Identity.accessToken)
  } yield IdentityHeaders(auth = auth, accessToken = token)

  override def retrieveSavedArticlesForUser(lambdaRequest: LambdaRequest): Future[Option[SavedArticles]] = {
    for{(key, value) <- lambdaRequest.headers} logger.info(s"Header name: ${key} value: ${value}")

    (for {
      identityHeaders <- getIdentityHeaders(lambdaRequest.headers)
    } yield {
      identityService.userFromRequest(identityHeaders).transformWith{
        case Success(Some(userId)) =>
          logger.info(s"Got user id ${userId} from identity")
          Future.fromTry(savedArticlesPersistence.read(userId))
        case Success(_) =>
          //TODO this needs fixig
          logger.info(s"no user found for AccessToken ${identityHeaders.accessToken}")
          Future.failed(new IllegalStateException("Attempted to retrieve articles for not existant user"))
        case Failure(_) =>
          logger.info(s"could not retrieve articles for: token: ${identityHeaders.accessToken}")
          Future.failed(new IllegalStateException("Attempted to retrieve articles for not existant user"))
      }
    }).getOrElse{
      logger.info(s"Could not retrieve identity headers")
      Future.failed(new IllegalStateException("Missing authorissation data"))
    }
  }
}
