package com.gu.sfl.services

import java.io.IOException

import com.gu.identity.client.IdentityApiClient
import com.gu.sfl.{Logging, Parallelism}
import com.gu.sfl.lambda.LambdaRequest
import okhttp3._
import org.apache.commons.httpclient.HttpClient
import com.gu.sfl.lib.Jackson._

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

case class IdentityConfig(identityApiHost: String)

case class IdentityToken(value: String)

case class IdentityHeaders(accessToken: String, auth: String)

trait IdentityService {
  def userFromRequest(identityHeaders: IdentityHeaders) : Future[Option[String]] //Will be Future[Option[UserId]]]
}


class IdentityServiceImpl(identityConfig: IdentityConfig, okHttpClient: OkHttpClient) extends IdentityService with Logging {
  implicit val executionContext: ExecutionContext = Parallelism.largeGlobalExecutionContext
  override def userFromRequest(identityHeaders: IdentityHeaders): Future[Option[String]] = {

    val meUrl = s"${identityConfig.identityApiHost}/user/me"
    logger.info(s"Attempting to retrieve articles with from: ${meUrl} with token: ${identityHeaders.accessToken}")

    val headers = new Headers.Builder()
      .add("X-GU-ID-Client-Access-Token", identityHeaders.accessToken)
      .add("Authorization", identityHeaders.auth)
      .build()

    val promise = Promise[Option[String]]

    okHttpClient.newCall(
      new Request.Builder()
        .url(meUrl)
        .headers(headers)
        .get()
        .build()
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

      override def onFailure(call: Call, e: IOException): Unit = promise.failure(e)
    })
    promise.future
  }
}
