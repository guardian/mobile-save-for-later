package com.gu.sfl.controller

import com.gu.sfl.exception.{IdentityServiceError, MissingAccessTokenError, UserNotFoundError}
import com.gu.sfl.lambda.{LambdaRequest, LambdaResponse}
import com.gu.sfl.lib.Base64Utils
import com.gu.sfl.lib.Jackson._
import com.gu.sfl.model.SavedArticles
import com.gu.sfl.savedarticles.UpdateSavedArticles
import com.gu.sfl.util.StatusCodes

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class SaveArticlesController(updateSavedArticles: UpdateSavedArticles)(implicit executionContext: ExecutionContext) extends Function[LambdaRequest, Future[LambdaResponse]] with SaveForLaterController with Base64Utils {

  override def defaultErrorMessage: String = "Error saving articles"

  override def apply(lambdaRequest: LambdaRequest): Future[LambdaResponse] = {
    val futureResponse = lambdaRequest match {
      case LambdaRequest(Some(json),  _) =>
        futureSave(Try.apply(mapper.readValue[SavedArticles](json)), lambdaRequest.headers)
      case LambdaRequest(None,  _) =>
        Future { LambdaResponse(StatusCodes.badRequest, Some("Expected a json body")) }
    }
    futureResponse
  }

  private def futureSave(triedRequest: Try[SavedArticles], requestHeaders: Map[String, String] ): Future[LambdaResponse] = {
    (for{
      articlestoSave <- Future.fromTry(triedRequest)
      maybeUpdatedArticles <- updateSavedArticles.save(requestHeaders, articlestoSave)
    }yield {
      maybeUpdatedArticles
    }).map {
      case Right(syncedPrefs) =>
        logger.debug("Got articles back from db")
        okSavedArticlesResponse(syncedPrefs)
      case Left(error) =>
        logger.error(s"Error saving articles: ${error.message}")
        processErrorResponse(error) {
          case i: IdentityServiceError =>  identityErrorResponse
          case m: MissingAccessTokenError => missingAccessTokenResponse
          case u: UserNotFoundError => missingUserResponse
        }
    }
  }
}
