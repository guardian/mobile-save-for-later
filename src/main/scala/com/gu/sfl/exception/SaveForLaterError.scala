package com.gu.sfl.exception

sealed trait SaveForLaterError {
  self: Throwable =>
}

case class MissingAccessToken(message: String) extends Exception(message) with SaveForLaterError
case class UserNotFoundException(message: String) extends Exception(message) with SaveForLaterError
case class SavedArticleMergeError(message: String) extends  Exception(message)
