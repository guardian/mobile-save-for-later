package com.gu.sfl.controller

import com.gu.sfl.exception.{MissingAccessTokenException, UserNotFoundException}
import com.gu.sfl.lambda.{LambdaRequest, LambdaResponse}
import com.gu.sfl.lib.Base64Utils
import com.gu.sfl.lib.Jackson._
import com.gu.sfl.model.{SavedArticles, SyncedPrefs, SyncedPrefsResponse}
import com.gu.sfl.savedarticles.UpdateSavedArticles
import com.gu.sfl.util.StatusCodes
import com.gu.sfl.{Logging, Parallelism}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SaveArticlesController(updateSavedArticles: UpdateSavedArticles) extends Function[LambdaRequest, Future[LambdaResponse]] with SaveForLaterController with Base64Utils with Logging {

  implicit val executionContext: ExecutionContext = Parallelism.largeGlobalExecutionContext

  override def apply(lambdaRequest: LambdaRequest): Future[LambdaResponse] = {
    logger.info("SaveForLaterController - handleReques")
    val futureRespons = lambdaRequest match {
      case LambdaRequest(Some(json),  _) =>
        logger.info("Save json as string")
        futureSave(Try(mapper.readValue(json, classOf[SavedArticles])), lambdaRequest.headers)

      case LambdaRequest(None,  _) =>
        logger.info("SaveForLaterController - bad request")
        Future { LambdaResponse(StatusCodes.badRequest, Some("Expected a json body")) }
    }
    futureRespons
  }

  private def futureSave(triedRequest: Try[SavedArticles], requestHeaders: Map[String, String] ): Future[LambdaResponse] = {
     (for{
       articlestoSave <- Future.fromTry(triedRequest)
       maybeSyncedPrefs <- updateSavedArticles.save(requestHeaders, articlestoSave)
     }yield {
       maybeSyncedPrefs
     }).transformWith {
       case Success(Some(syncedPrefs)) =>
         logger.info("Got articles back from db")
         Future { okSavedArticlesResponse(syncedPrefs) }
       case Success(None) =>
          logger.info("No articles found for user")
          Future { emptyArticlesResponse }
       case Failure(t: Throwable) =>
          logger.info(s"Error saving articles: ${t.getMessage}")
          t match {
            case m: MissingAccessTokenException => Future{ missingAccessTokenResponse }
            case u: UserNotFoundException => Future{ missingUserResponse }
            case _ => Future { serverError }
          }
     }
  }

}
