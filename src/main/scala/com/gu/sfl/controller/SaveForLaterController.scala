package com.gu.sfl.controller

import com.gu.sfl.Logging
import com.gu.sfl.exception.SaveForLaterError
import com.gu.sfl.lambda.LambdaResponse
import com.gu.sfl.lib.Jackson.mapper
import com.gu.sfl.model._
import com.gu.sfl.util.StatusCodes

import scala.concurrent.Future

trait SaveForLaterController extends Logging {

  def defaultErrorMessage: String

  def lambdaErrorResponse(statusCode:Int, errors: List[Error]) = LambdaResponse(statusCode, Some(mapper.writeValueAsString(ErrorResponse(errors = errors))))

  val missingUserResponse = lambdaErrorResponse(StatusCodes.forbidden, List(Error("Access Denied", "Access Denied")))
  val missingAccessTokenResponse = lambdaErrorResponse(StatusCodes.forbidden, List(Error("Access denied", "could not find an access token.")))
  val identityErrorResponse = lambdaErrorResponse(StatusCodes.internalServerError, List(Error("Access denied","Could not retrieve user id.")))
  val emptyArticlesResponse = LambdaResponse(StatusCodes.ok, Some(mapper.writeValueAsString(SavedArticles(List.empty))))

  def maximumSavedArticlesErrorResponse(message: String) = lambdaErrorResponse(StatusCodes.entityTooLarge, (List(Error("Payload too large", message))))
  def okSyncedPrefsResponse(syncedPrefs: SyncedPrefs): LambdaResponse = LambdaResponse(StatusCodes.ok, Some(mapper.writeValueAsString(SyncedPrefsResponse("ok", syncedPrefs))))
  def okSavedArticlesResponse(savedArticles: SavedArticles): LambdaResponse = LambdaResponse(StatusCodes.ok, Some(mapper.writeValueAsString(SavedArticlesResponse("ok", savedArticles))))
  def serverErrorResponse(message: String) = lambdaErrorResponse(StatusCodes.internalServerError, List(Error("Server error.", message)))

  def processErrorResponse(error: SaveForLaterError)(errorResolutions: PartialFunction[SaveForLaterError, LambdaResponse] = PartialFunction.empty) : LambdaResponse = {
    val errorResponseMaker = errorResolutions.orElse[SaveForLaterError, LambdaResponse] {
      case _ =>
        logger.error(s"Could not find correct response for: ${error}")
        serverErrorResponse(defaultErrorMessage)
    }
    errorResponseMaker(error)
  }
}
