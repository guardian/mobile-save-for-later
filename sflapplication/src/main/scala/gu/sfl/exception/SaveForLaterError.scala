package sfl.exception

sealed trait SaveForLaterError {
  def message: String
}

case class MissingAccessTokenError(message: String) extends SaveForLaterError
case class UserNotFoundError(message: String) extends SaveForLaterError
case class IdentityServiceError(message: String) extends SaveForLaterError
case class SavedArticleMergeError(message: String) extends  SaveForLaterError
case class RetrieveSavedArticlesError(message: String) extends SaveForLaterError


case class IdentityApiRequestError(message: String) extends Exception(message)
