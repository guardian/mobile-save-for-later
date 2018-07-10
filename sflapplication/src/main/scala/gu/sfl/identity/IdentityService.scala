package sfl.identity

import java.io.IOException

import sfl.exception.IdentityApiRequestError
import sfl.lib.Jackson._
import com.gu.sfl.Logging
import okhttp3._

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

case class IdentityConfig(identityApiHost: String)

case class IdentityHeader(auth: String, accessToken: String = "Bearer application_token")

trait IdentityService {
  def userFromRequest(identityHeaders: IdentityHeader) : Future[Option[String]]
}
class IdentityServiceImpl(identityConfig: IdentityConfig, okHttpClient: OkHttpClient)(implicit executionContext: ExecutionContext) extends IdentityService with Logging {

  override def userFromRequest(identityHeaders: IdentityHeader): Future[Option[String]] = {

    val meUrl = s"${identityConfig.identityApiHost}/user/me"

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
        Try {
           val body = response.body().string()
           logger.debug(s"Identity api response: $body")
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
