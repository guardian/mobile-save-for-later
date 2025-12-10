package com.gu.sfl.controller

import com.gu.identity.auth.{InvalidOrExpiredToken, MissingRequiredClaim, MissingRequiredScope}
import com.gu.sfl.exception.{IdentityServiceError, MissingAccessTokenError, OktaOauthValidationError, IdentityUserNotFoundError}
import com.gu.sfl.lambda.{LambdaRequest, LambdaResponse}
import com.gu.sfl.savedarticles.FetchSavedArticles
import com.gu.sfl.util.StatusCodes

import scala.concurrent.{ExecutionContext, Future}

class FetchArticlesController(fetchSavedArticles: FetchSavedArticles)(implicit executionContext: ExecutionContext) extends Function[LambdaRequest, Future[LambdaResponse]] with SaveForLaterController  {

  override def defaultErrorMessage: String = "Error fetching articles"

  override def apply(lambdaRequest: LambdaRequest): Future[LambdaResponse] = {

     val futureResponse =  fetchSavedArticles.retrieveForUser(lambdaRequest.headers).map {
       case Right(syncedPrefs) =>
         syncedPrefs.savedArticles.foreach ( sp =>
            logger.debug(s"Returning found: ${sp.articles.size} articles")
         )
         okSyncedPrefsResponse(syncedPrefs)
       case Left(error) =>
         val appInfo = lambdaRequest.headers.map { case (k, v) => k.toLowerCase -> v }.getOrElse("user-agent", "user-agent header not found")
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
     futureResponse
  }
}
