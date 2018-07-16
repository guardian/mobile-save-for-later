package com.gu.sfl.identity

import java.io.IOException

import com.gu.sfl.exception.IdentityApiRequestError
import okhttp3._
import org.specs2.matcher.ThrownMessages
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import com.gu.sfl.identity.IdentityServiceImpl
import com.gu.sfl.lib.GlobalHttpClient

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}


class IdentityServiceSpec extends Specification with ThrownMessages with Mockito {

  val identityHeaders = IdentityHeader("auth", "access-token")

  "the identity service" should {

    "return the user id when the identity api returns it" in new MockHttpRequestScope {
      val futureUserId = identityService.userFromRequest(identityHeaders)
      Await.result(futureUserId, Duration.Inf) mustEqual (Some("1234"))
    }

    "return none when the user id is not found" in new MockBadIdResponseScope {
      val futureUserId = identityService.userFromRequest(identityHeaders)
      Await.result(futureUserId, Duration.Inf) mustEqual (None)
    }

    "the exception is caught when the request to identity fails" in new IdentityRequestFailsScope {
      val idFailResult =  Await.ready(identityService.userFromRequest(identityHeaders), Duration.Inf).value.get

      idFailResult match {
        case Success(_) => fail("No IOxception thrown")
        case Failure(e) => e mustEqual(IdentityApiRequestError("Did not get identiy api response"))
      }

    }
  }

  trait IdentityRequestFailsScope extends MockHttpRequestScope {
    val exception = new IOException("Id api splurt")
    override def theCallBack(callback: Callback, request: Request): Unit = {
      callback.onFailure(call, exception)
    }
  }

  trait MockBadIdResponseScope extends MockHttpRequestScope {
    override val body = """{"status":"error","errors":[{"message":"Access Denied","description":"Access Denied"}]}"""
    override val code = 403
  }

  trait MockHttpRequestScope extends Scope {

    def buildResponse(request: Request, code: Int, body: Array[Byte]): Response = {
      ResponseBody.create(GlobalHttpClient.applicationJsonMediaType, body)
      new Response.Builder()
        .body(ResponseBody.create(GlobalHttpClient.applicationJsonMediaType, body))
        .code(code)
        .protocol(Protocol.HTTP_1_1)
        .request(request)
        .message("status message")
        .build()
    }

    //This is a cutdown of what the actual id response is
    def body: String = """{ "status": "ok", "user": { "id": "1234" 	} }"""
    def code: Int = 200

    val httpClient = mock[OkHttpClient]
    val call = mock[Call]

    def theCallBack(callback: Callback, request: Request): Unit = {
      callback.onResponse(
        call,
        buildResponse(request, code, body.getBytes)
      )
    }

    httpClient.newCall(any[Request]()) answers {
    (_: Any) match {
      case (request: Request) => {
        call.enqueue(any[Callback]()) answers {
          (_: Any) match {
            case (callback: Callback) => theCallBack(callback, request)
          }
        }
        call
      }
    }
  }
    val identityService = new IdentityServiceImpl(IdentityConfig(identityApiHost = "https://guardianidentiy.com"), httpClient)
  }

}
