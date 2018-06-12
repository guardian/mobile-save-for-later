package com.gu.sfl.controller

import com.gu.sfl.exception.{IdentityServiceException, MaxSavedArticleTransgressionError, MissingAccessTokenException, UserNotFoundException}
import com.gu.sfl.lambda.{LambdaRequest, LambdaResponse}
import com.gu.sfl.lib.Base64Utils
import com.gu.sfl.lib.Jackson._
import com.gu.sfl.model.{SavedArticles, SyncedPrefs, SyncedPrefsResponse}
import com.gu.sfl.savedarticles.UpdateSavedArticles
import com.gu.sfl.util.StatusCodes
import com.gu.sfl.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SaveArticlesController(updateSavedArticles: UpdateSavedArticles)(implicit executionContext: ExecutionContext) extends Function[LambdaRequest, Future[LambdaResponse]] with SaveForLaterController with Base64Utils with Logging {

  override def apply(lambdaRequest: LambdaRequest): Future[LambdaResponse] = {
    logger.info("SaveForLaterController - handleReques")
    val futureResponse = lambdaRequest match {
      case LambdaRequest(Some(json),  _) =>
        futureSave(Try.apply(mapper.readValue(json, classOf[SavedArticles])), lambdaRequest.headers)
      case LambdaRequest(None,  _) =>
        Future { LambdaResponse(StatusCodes.badRequest, Some("Expected a json body")) }
    }
    futureResponse
  }

  private def futureSave(triedRequest: Try[SavedArticles], requestHeaders: Map[String, String] ): Future[LambdaResponse] = {
     (for{
       articlestoSave <- Future.fromTry(triedRequest)
       maybeSyncedPrefs <- updateSavedArticles.save(requestHeaders, articlestoSave)
     }yield {
       maybeSyncedPrefs
     }).transformWith {
       case Success(Some(syncedPrefs)) =>
         logger.debug("Got articles back from db")
         Future { okSavedArticlesResponse(syncedPrefs) }
       case Success(None) =>
          logger.debug("No articles found for user")
          Future { emptyArticlesResponse }
       case Failure(t: Throwable) =>
          logger.debug(s"Error saving articles: ${t.getMessage}")
          t match {
            case i: IdentityServiceException => Future { identityErrorResponse }
            case m: MissingAccessTokenException => Future{ missingAccessTokenResponse }
            case u: UserNotFoundException => Future{ missingUserResponse }
            case m: MaxSavedArticleTransgressionError => Future { maximumSavedArticlesErrorResponse }
            case _ => Future { serverErrorResponse }
          }
     }
  }

}
