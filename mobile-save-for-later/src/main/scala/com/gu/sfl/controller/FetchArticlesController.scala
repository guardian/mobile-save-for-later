package com.gu.sfl.controller

import com.gu.identity.auth.{InvalidOrExpiredToken, MissingRequiredClaim, MissingRequiredScope}
import com.gu.sfl.exception.{IdentityServiceError, MissingAccessTokenError, OktaOauthValidationError, UserNotFoundError}
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
         logger.error(s"Error trying to retrieve saved articles ($appInfo): ${error.message}")
         processErrorResponse(error) {
           case e: IdentityServiceError =>  identityErrorResponse
           case e: MissingAccessTokenError => missingAccessTokenResponse
           case e: UserNotFoundError => missingUserResponse
           case OktaOauthValidationError(e, InvalidOrExpiredToken) => oktaOauthError(e, StatusCodes.unauthorized)
           case OktaOauthValidationError(e, MissingRequiredClaim(_)) => oktaOauthError(e, StatusCodes.badRequest)
           case OktaOauthValidationError(e, MissingRequiredScope(_)) => oktaOauthError(e, StatusCodes.forbidden)
           case OktaOauthValidationError(e, _) => oktaOauthError(e, StatusCodes.unauthorized)
         }
     }
     futureResponse
  }
}
