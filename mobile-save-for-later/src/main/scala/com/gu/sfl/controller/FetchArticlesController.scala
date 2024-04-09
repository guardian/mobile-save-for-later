package com.gu.sfl.controller

import com.gu.identity.auth.{InvalidOrExpiredToken, MissingRequiredClaim, MissingRequiredScope}
import com.gu.sfl.exception.{IdentityServiceError, MissingAccessTokenError, OktaOauthValidationError, UserNotFoundError}
import com.gu.sfl.lambda.{LambdaRequest, LambdaResponse}
import com.gu.sfl.savedarticles.FetchSavedArticles
import com.gu.sfl.util.StatusCodes

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

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
         logger.error(s"Error trying to retrieve saved articles: ${error.message}")
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

  def runLocally = {
    val headers = Map("authorization" -> "Bearer eyJraWQiOiJVeWRLRmFhVG41ekVPLUNtTWV4WFJ3aldOU0MwS2pndjJlVWgwVWt2UHN3IiwiYWxnIjoiUlMyNTYifQ.eyJ2ZXIiOjEsImp0aSI6IkFULkQ2d29XX3pYRmFNN1l6U2VUMTRrM1kwSFdqUDRCVnB5M1RtcGtqMlgwc0UiLCJpc3MiOiJodHRwczovL3Byb2ZpbGUuY29kZS5kZXYtdGhlZ3VhcmRpYW4uY29tL29hdXRoMi9hdXMzdjlnbGE5NVRvajBFRTB4NyIsImF1ZCI6Imh0dHBzOi8vcHJvZmlsZS5jb2RlLmRldi10aGVndWFyZGlhbi5jb20vIiwic3ViIjoibGluZHNleS5kZXdAZ3VhcmRpYW4uY28udWsiLCJpYXQiOjE3MTI2NTIxNjMsImV4cCI6MTcxMjY1NTc2MywiY2lkIjoiMG9hNGl5ang2OTJBajhTbFoweDciLCJ1aWQiOiIwMHU0djIxNzF4d1R3bGIyTTB4NyIsInNjcCI6WyJwcm9maWxlIiwib3BlbmlkIiwiZ3VhcmRpYW4uc2F2ZS1mb3ItbGF0ZXIudXBkYXRlLnNlbGYiLCJndWFyZGlhbi5zYXZlLWZvci1sYXRlci5yZWFkLnNlbGYiXSwiYXV0aF90aW1lIjoxNzExMDQwNzY4LCJpZGVudGl0eV91c2VybmFtZSI6IiIsImVtYWlsX3ZhbGlkYXRlZCI6dHJ1ZSwibGVnYWN5X2lkZW50aXR5X2lkIjoiMjAwMDgyNzY5In0.M9TjUMrVWB4eV3yLJ1nGIm10Q8wPEGNfcz4aR0_UewUMmru8olFD3dijfs54VuOLWY6Cyou9hPgXZnaLPevudgdHDbYiuAhFt4cUKk9hvgbBcojd_a65bcHn6aV1gd2os3NOGOacpB7hpHK4Q37mk_DNGRvdSo2YkSmycAxG_Zfr6BdC7uxxL-COQNvdgi7zEX4d0n0xsaOPMYoau4yBGusDGPECkSrqHbfDsajInz1gjHDPpqiovo5csI-5Wjlrl9dhV0sKggGzagasEw_aaDc7LZkPVklCUdXv299_37_m_6uJuN-dvxBlOt_VthiXZ2dk1g3IW-sarLCqQ7qN4Q", "x-gu-is-oauth"-> "true")
    val response = Await.ready(fetchSavedArticles.retrieveForUser(headers), 5 seconds).value.get
    response match {
      case Success(value) =>  value match {
        case Right(syncedPrefs) => println(syncedPrefs)
        case Left(error) => println(s"Error trying to retrieve saved articles: ${error.message}")
      }
      case Failure(exception) => println(exception.getMessage)
    }
  }
}
