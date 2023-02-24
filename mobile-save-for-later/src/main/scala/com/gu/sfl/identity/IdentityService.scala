package com.gu.sfl.identity

import com.gu.identity.auth.{DefaultAccessClaims, MissingOrInvalidHeader, OktaLocalValidator, OktaValidationException, ValidationError, AccessScope => IdentityAccessScope}

import java.io.IOException
import com.gu.sfl.Logging
import com.gu.sfl.exception.IdentityApiRequestError
import com.gu.sfl.lib.Jackson._
import okhttp3._

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

case class IdentityConfig(identityApiHost: String)

trait IdentityHeaders {
  val accessToken: String
  val isOauth: Boolean
}

case class IdentityHeadersWithAuth(auth: String, accessToken: String = "Bearer application_token", isOauth: Boolean = false) extends IdentityHeaders

case class IdentityHeadersWithCookie(scGuUCookie: String, accessToken: String, isOauth: Boolean = false) extends IdentityHeaders

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
  def userFromRequest(identityHeaders: IdentityHeaders, requiredScope: List[IdentityAccessScope]) : Future[Option[String]]
}
class IdentityServiceImpl(identityConfig: IdentityConfig, okHttpClient: OkHttpClient, oktaLocalValidator: OktaLocalValidator[DefaultAccessClaims])(implicit executionContext: ExecutionContext) extends IdentityService with Logging {
  def userFromRequestIdapi(identityHeaders: IdentityHeaders): Future[Option[String]] = {
    val meUrl = s"${identityConfig.identityApiHost}/user/me/identifiers"

    val headers = identityHeaders match {
      case auth: IdentityHeadersWithAuth => new Headers.Builder()
        .add("X-GU-ID-Client-Access-Token", identityHeaders.accessToken)
        .add("Authorization", auth.auth)
        .build()
      case cookie: IdentityHeadersWithCookie => new Headers.Builder()
        .add("X-GU-ID-Client-Access-Token", identityHeaders.accessToken)
        .add("X-GU-ID-FOWARDED-SC-GU-U", cookie.scGuUCookie)
        .build()
    }

    val promise = Promise[Option[String]]

    val request = new Request.Builder()
      .url(meUrl)
      .headers(headers)
      .get()
      .build()

    def extractUserIdFromSuccessResult(response: Response) = {
      Try {
        val body = response.body().string()
        logger.debug(s"Identity api response: $body")
        val node = mapper.readTree(body.getBytes)
        node.path("id").textValue
      } match {
        case Success(userId) =>
          // .textValue can return null, so wrap in Option.apply
          promise.success(Option(userId))
        case Failure(_) =>
          promise.success(None)
      }
    }

    okHttpClient.newCall(
      request
    ).enqueue(new Callback {
      override def onResponse(call: Call, response: Response): Unit = {
        val responseCodeGroup = response.code() / 100

        // 5XX responses should not log users out, so fail the promise instead of returning None
        if (responseCodeGroup == 5) {
          promise.failure(IdentityApiRequestError("Identity api server error"))
        } else if (responseCodeGroup == 2) {
          extractUserIdFromSuccessResult(response)
        } else {
          promise.success(None)
        }
      }

      override def onFailure(call: Call, e: IOException): Unit = promise.failure(IdentityApiRequestError("Did not get identiy api response"))
    })
    promise.future
  }

  def userFromRequestOauth(identityHeaders: IdentityHeadersWithAuth, requiredScope: List[IdentityAccessScope]): Either[ValidationError, DefaultAccessClaims] =
    oktaLocalValidator.claimsFromAccessToken(identityHeaders.auth.stripPrefix("Bearer "), requiredScope)

  override def userFromRequest(identityHeaders: IdentityHeaders, requiredScope: List[IdentityAccessScope]): Future[Option[String]] = {
    if (identityHeaders.isOauth) {
      identityHeaders match {
        case auth: IdentityHeadersWithAuth =>
          userFromRequestOauth(auth, requiredScope) match {
            case Left(e) => Future.failed(OktaValidationException(e))
            case Right(claims) => Future.successful(Some(claims.identityId))
          }
        case _ => Future.failed(OktaValidationException(MissingOrInvalidHeader))
      }
    } else {
      identityHeaders match {
        case auth: IdentityHeadersWithAuth => userFromRequestIdapi(auth)
        case cookie: IdentityHeadersWithCookie => userFromRequestIdapi(cookie)
      }
    }


  }
}
