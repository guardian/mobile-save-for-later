package com.gu.sfl.controller

import com.gu.identity.auth.{InvalidOrExpiredToken, MissingRequiredClaim, MissingRequiredScope}
import com.gu.sfl.exception.{IdentityServiceError, MissingAccessTokenError, OktaOauthValidationError, IdentityUserNotFoundError}
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
        logger.info("Got articles back from db")
        okSavedArticlesResponse(syncedPrefs)
      case Left(error) =>
         val appInfo = requestHeaders.map { case (k, v) => k.toLowerCase -> v }.getOrElse("user-agent", "user-agent header not found")
        processErrorResponse(error) {

          case i: IdentityServiceError =>  {
            val errorCode = StatusCodes.internalServerError
            logger.error(s"Identity server error ($errorCode): ${i.message}. ($appInfo)")
            identityErrorResponse
          }
          case m: MissingAccessTokenError => {
            val errorCode = StatusCodes.forbidden
            logger.error(s"No access token on the request ($errorCode). ($appInfo)")
            missingAccessTokenResponse
          }
          case u: IdentityUserNotFoundError => {
            val errorCode = StatusCodes.forbidden
            logger.error(s"Token did not contain a valid identity user id ($errorCode). ($appInfo)")
            missingUserResponse
          }
          case OktaOauthValidationError(e, InvalidOrExpiredToken) => {
            val errorCode = StatusCodes.unauthorized
            logger.error(s"Auth token invalid or expired ($errorCode): $e. ($appInfo)")
            oktaOauthError(e, errorCode)
          }
          case OktaOauthValidationError(e, MissingRequiredClaim(_)) => {
            val errorCode = StatusCodes.badRequest
            logger.error(s"Auth token missing the required claim ($errorCode): $e. ($appInfo)")
            oktaOauthError(e, StatusCodes.badRequest)
          }
          case OktaOauthValidationError(e, MissingRequiredScope(_)) => {
            val errorCode = StatusCodes.forbidden
            logger.error(s"Auth token missing the required scope ($errorCode): $e. ($appInfo)")
            oktaOauthError(e, errorCode)
          }
          case OktaOauthValidationError(e, _) => {
            val errorCode = StatusCodes.unauthorized
            logger.error(s"Auth error ($errorCode): $e. ($appInfo)")
            oktaOauthError(e, errorCode)
          }
         }
    }
  }
}
