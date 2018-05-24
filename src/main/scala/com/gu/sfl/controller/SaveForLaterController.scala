package com.gu.sfl.controller

import com.gu.sfl.lambda.LambdaResponse
import com.gu.sfl.lib.Jackson.mapper
import com.gu.sfl.model.{SavedArticles, SyncedPrefs, SyncedPrefsResponse}
import com.gu.sfl.util.StatusCodes

trait SaveForLaterController {
  val missingUserResponse = LambdaResponse(StatusCodes.badRequest, Some("Could not find a user "))
  val missingAccessTokenResponse = LambdaResponse(StatusCodes.badRequest, Some("could not find an access token"))
  val serverError = LambdaResponse(StatusCodes.internalServerError, Some("Server error"))
  val emptyArticlesResponse = LambdaResponse(StatusCodes.ok, Some(mapper.writeValueAsString(SavedArticles(List.empty))))
  def okSavedArticlesResponse(syncedPrefs: SyncedPrefs): LambdaResponse = LambdaResponse(StatusCodes.ok, Some(mapper.writeValueAsString(SyncedPrefsResponse("ok", syncedPrefs))))
}
