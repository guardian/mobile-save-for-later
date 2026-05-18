package com.gu.sfl.identity

import com.gu.identity.auth.{AccessToken, DefaultAccessClaims, OktaLocalAccessTokenValidator, OktaValidationException, ValidationError, AccessScope => IdentityAccessScope}
import com.gu.sfl.Logging

import java.security.MessageDigest
import java.util.HexFormat
import scala.concurrent.Future

case class IdentityHeader(auth: String, accessToken: String = "Bearer application_token", isOauth: Boolean = false)

object AccessScope {
  /**
   * Allows the client to read the user's saved for later articles, used by the FetchSavedArticles lambda
   */
  case object readSelf extends IdentityAccessScope {
    val name = "guardian.save-for-later.read.self"
  }

  /**
   * Allows the client to update the user's saved for later articles, used by the UpdateSavedArticles lambda
   */
  case object updateSelf extends IdentityAccessScope {
    val name = "guardian.save-for-later.update.self"
  }
}

trait IdentityService {
  def userFromRequest(identityHeaders: IdentityHeader, requiredScope: List[IdentityAccessScope]) : Future[Option[String]]
}
class IdentityServiceImpl(oktaLocalAccessTokenValidator: OktaLocalAccessTokenValidator) extends IdentityService with Logging {
  def userFromRequestIdapi(identityHeaders: IdentityHeader): Future[Option[String]] = {
    val tokenBytes = MessageDigest.getInstance("SHA-256").digest(identityHeaders.auth.getBytes("UTF-8"))
    logger.warn(s"Detected legacy User Access Token instead of Okta Access Token. Hash: ${HexFormat.of().formatHex(tokenBytes)}" )
    Future.successful(None)
  }

  def userFromRequestOauth(identityHeaders: IdentityHeader, requiredScope: List[IdentityAccessScope]): Either[ValidationError, DefaultAccessClaims] =
    oktaLocalAccessTokenValidator.parsedClaimsFromAccessToken(
      AccessToken(identityHeaders.auth.stripPrefix("Bearer ")),
      requiredScope
    )

  override def userFromRequest(identityHeaders: IdentityHeader, requiredScope: List[IdentityAccessScope]): Future[Option[String]] = {
    identityHeaders.isOauth match {
      case true => userFromRequestOauth(identityHeaders, requiredScope) match {
        case Left(e) => Future.failed(OktaValidationException(e))
        case Right(claims) => Future.successful(Some(claims.identityId))
      }
      case false => userFromRequestIdapi(identityHeaders)
    }
  }
}
