package com.gu.sfl.exception

import com.gu.identity.auth.ValidationError

sealed trait SaveForLaterError {
  def message: String
}

case class MissingAccessTokenError(message: String) extends SaveForLaterError
case class IdentityUserNotFoundError(message: String) extends SaveForLaterError
case class IdentityServiceError(message: String) extends SaveForLaterError
case class SavedArticleMergeError(message: String) extends  SaveForLaterError
case class RetrieveSavedArticlesError(message: String) extends SaveForLaterError

case class OktaOauthValidationError(message: String, validationError: ValidationError) extends SaveForLaterError


case class IdentityApiRequestError(message: String) extends Exception(message)
