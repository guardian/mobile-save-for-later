package com.gu.sfl.controller

import com.gu.sfl.lambda.LambdaResponse
import com.gu.sfl.lib.Jackson.mapper
import com.gu.sfl.model.{SavedArticles, SavedArticlesResponse, SyncedPrefs, SyncedPrefsResponse}
import com.gu.sfl.util.StatusCodes

trait SaveForLaterController {
  val missingUserResponse = LambdaResponse(StatusCodes.badRequest, Some("Could not find a user "))
  val maximumSavedArticlesErrorResponse = LambdaResponse(StatusCodes.badRequest, Some("Maximum saved articles exceeded."))
  val missingAccessTokenResponse = LambdaResponse(StatusCodes.badRequest, Some("could not find an access token."))
  val identityErrorResponse = LambdaResponse(StatusCodes.internalServerError, Some("Could not retrieve user id."))
  val serverErrorResponse = LambdaResponse(StatusCodes.internalServerError, Some("Server error."))
  val accessDenied = LambdaResponse(StatusCodes.forbidden, Some("Access denied"))
  val emptyArticlesResponse = LambdaResponse(StatusCodes.ok, Some(mapper.writeValueAsString(SavedArticles(List.empty))))
  def okSyncedPrefsResponse(syncedPrefs: SyncedPrefs): LambdaResponse = LambdaResponse(StatusCodes.ok, Some(mapper.writeValueAsString(SyncedPrefsResponse("ok", syncedPrefs))))
  def okSavedArticlesResponse(savedArticles: SavedArticles): LambdaResponse = LambdaResponse(StatusCodes.ok, Some(mapper.writeValueAsString(SavedArticlesResponse("ok", savedArticles))))
}
