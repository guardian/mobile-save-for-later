import java.io.IOException

import com.gu.sfl.lib.Jackson._
import okhttp3._

import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class LoggingInterceptor extends Interceptor {
  override def intercept(chain: Interceptor.Chain) = {
    val request = chain.request

    val t1: Long = System.nanoTime
    println(s"Sending request ${request.url} on ${chain.connection()}\nHeaders: ${request.headers}")

    val response: Response = chain.proceed(request)

    response
  }
}

val client = new OkHttpClient.Builder()
      .addInterceptor(new LoggingInterceptor)
  .build()

val url = "https://id.guardianapis.com/user/me"

val headers = new Headers.Builder()
  .add("X-GU-ID-Client-Access-Token","Bearer application_token")
  .add("Authorization", "Bearer 54aad37356f752b84fd78da27776130103795fc3197bfb993e53a89e35782fdc4")


val req = new Request.Builder()
    .headers(headers.build())
    .url(url)
    .get()
    .build()


val promise = Promise[Option[String]]
client.newCall(req).enqueue(new Callback {

  override def onResponse(call: Call, response: Response): Unit = {
    val body: ResponseBody = response.body()
    val bodyContents = body.string
    println(s"BODY::: ${bodyContents} st: ${response.code}")
    Try {
      val node = mapper.readTree(bodyContents.getBytes())
      node.get("user").path("id").toString
    } match {
      case Success(userId) => promise.success(Some(userId))
      case Failure(t) => promise.failure(t)
    }
  }

  override def onFailure(call: Call, e: IOException): Unit = promise.failure(e)
})

val res = promise.future

Await.result(res, 10 seconds)



