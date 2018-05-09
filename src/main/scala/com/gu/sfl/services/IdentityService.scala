package com.gu.sfl.services

import java.io.IOException

import com.gu.sfl.exception.IdentityApiRequestError
import com.gu.sfl.{Logging, Parallelism}
import com.gu.sfl.lambda.LambdaRequest
import okhttp3._
import com.gu.sfl.lib.Jackson._

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

case class IdentityConfig(identityApiHost: String)

case class IdentityHeaders(auth: String, accessToken: String = "Bearer application_token")

trait IdentityService {
  def userFromRequest(identityHeaders: IdentityHeaders) : Future[Option[String]]
}
class IdentityServiceImpl(identityConfig: IdentityConfig, okHttpClient: OkHttpClient) extends IdentityService with Logging {

  implicit val executionContext: ExecutionContext = Parallelism.largeGlobalExecutionContext
  override def userFromRequest(identityHeaders: IdentityHeaders): Future[Option[String]] = {

    val meUrl = s"${identityConfig.identityApiHost}/user/me"
    logger.info(s"Attempting to get user details with from: ${meUrl} with token: ${identityHeaders.accessToken}")

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

    okHttpClient.newCall(
      request
    ).enqueue(new Callback {
      override def onResponse(call: Call, response: Response): Unit = {
         val body = response.body().string()
         logger.info(s"Identity api response: $body")
         Try {
           val node = mapper.readTree(body.getBytes)
           node.get("user").path("id").textValue
         } match {
           case Success(userId) => promise.success(Some(userId))
           case Failure(_)  => promise.success(None)
         }
      }

      override def onFailure(call: Call, e: IOException): Unit = promise.failure(IdentityApiRequestError("Did not get identiy api response"))
    })
    promise.future
  }
}
