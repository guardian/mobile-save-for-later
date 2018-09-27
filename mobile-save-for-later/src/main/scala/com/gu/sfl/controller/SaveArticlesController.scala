package com.gu.sfl.controller

import com.gu.sfl.exception.{IdentityServiceError, MissingAccessTokenError, UserNotFoundError}
import com.gu.sfl.lambda.{LambdaRequest, LambdaResponse}
import com.gu.sfl.lib.Base64Utils
import com.gu.sfl.lib.Jackson._
import com.gu.sfl.model.{DirtySavedArticles, SavedArticles}
import com.gu.sfl.savedarticles.UpdateSavedArticles
import com.gu.sfl.util.StatusCodes

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

class SaveArticlesController(updateSavedArticles: UpdateSavedArticles)(implicit executionContext: ExecutionContext) extends Function[LambdaRequest, Future[LambdaResponse]] with SaveForLaterController with Base64Utils {
  private val headersToKeep: Set[String] = Set("user-agent", "content-type", "content-length", "accept", "accept-encoding", "x-forwarded-host").map(_.toLowerCase())
  override def defaultErrorMessage: String = "Error saving articles"

  override def apply(lambdaRequest: LambdaRequest): Future[LambdaResponse] = {
    val futureResponse = lambdaRequest match {
      case LambdaRequest(Some(json),  _) =>
        val triedSavedArticles = Try{
          SavedArticles(mapper.readValue[DirtySavedArticles](json))
        }
        triedSavedArticles match {
          case Failure(t) => {
            val headersWithoutAuth = lambdaRequest.headers.filter{ case (k,v) => headersToKeep.contains(k.toLowerCase)}
            logger.warn(s"Could not read value: $json \nWith headers: $headersWithoutAuth" )
          }

          case _ => ()
        }
        futureSave(triedSavedArticles, lambdaRequest.headers)
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
