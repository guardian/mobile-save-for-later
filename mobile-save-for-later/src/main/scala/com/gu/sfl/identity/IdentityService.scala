package com.gu.sfl.identity

import com.gu.identity.auth.{DefaultAccessClaims, OktaLocalValidator, OktaValidationException, AccessScope => IdentityAccessScope}

import java.io.IOException
import com.gu.sfl.Logging
import com.gu.sfl.exception.{IdentityApiRequestError}
import com.gu.sfl.lib.Jackson._
import okhttp3._

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

case class IdentityConfig(identityApiHost: String)

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
class IdentityServiceImpl(identityConfig: IdentityConfig, okHttpClient: OkHttpClient, oktaLocalValidator: OktaLocalValidator[DefaultAccessClaims])(implicit executionContext: ExecutionContext) extends IdentityService with Logging {
  def userFromRequestIdapi(identityHeaders: IdentityHeader): Future[Option[String]] = {
    val meUrl = s"${identityConfig.identityApiHost}/user/me/identifiers"

    val headers = new Headers.Builder()
      .add("X-GU-ID-Client-Access-Token", identityHeaders.accessToken)
      .add("Authorization", identityHeaders.auth)
      .build()

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

  def userFromRequestOauth(identityHeaders: IdentityHeader, requiredScope: List[IdentityAccessScope]): Future[Option[String]] = {
    val promise = Promise[Option[String]]
    val claims = oktaLocalValidator.claimsFromAccessToken(identityHeaders.auth.stripPrefix("Bearer "), requiredScope)
    println(claims.toString)
    claims match {
      case Left(e) => {
        promise.failure(OktaValidationException(e))
      }
      case Right(c) => promise.success(Option(c.identityId))
    }

    promise.future
  }

  override def userFromRequest(identityHeaders: IdentityHeader, requiredScope: List[IdentityAccessScope]): Future[Option[String]] = {
    identityHeaders.isOauth match {
      case true => userFromRequestOauth(identityHeaders, requiredScope)
      case false => userFromRequestIdapi(identityHeaders)
    }
  }
}
