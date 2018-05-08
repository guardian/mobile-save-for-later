package com.gu.sfl.lib

import com.gu.sfl.Parallelism
import com.gu.sfl.services.{IdentityConfig, IdentityHeaders, IdentityService, IdentityServiceImpl}
import okhttp3.{ Call, Callback, OkHttpClient, Request, Response, ResponseBody, Protocol }
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.specs2.mutable.Specification
import org.mockito.{ArgumentCaptor, Matchers}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.specs2.specification.Scope

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import org.specs2.mock.Mockito




class IdentityServiceSpec extends Specification with Mockito {

  implicit val executionContext: ExecutionContext = Parallelism.largeGlobalExecutionContext
  val identityHeaders = IdentityHeaders("auth", "access-token")

  "the identity service" should {

      "return the user id when the identity api returns it" in new MockGoodIdResponseScope {
         val identityService = new IdentityServiceImpl(IdentityConfig(identityApiHost = "https://guardianidentiy.com"), httpClient)
         val futureUserId = identityService.userFromRequest(identityHeaders)
         Await.result(futureUserId, Duration.Inf) mustEqual(Some("1234"))
      }

  }

  trait MockGoodIdResponseScope extends MockHttpRequestScope {

    //This is a cutdown of what the actual id response is
    val body = """{
                 |	"status": "ok",
                 |	"user": {
                 |		"id": "1234"
                 |	}
                 |}""".stripMargin

     def theCallBack(callback: Callback, request: Request): Unit = {
       callback.onResponse(
         call,
         buildResponse(request, 200, body.getBytes)
       )
    }
  }

  trait MockHttpRequestScope extends Scope {

    def buildResponse(request: Request, code: Int, body: Array[Byte]): Response = {
      ResponseBody.create(GlobalHttpClient.applicationJsonMediaType, body)
      new Response.Builder()
        .body(ResponseBody.create(GlobalHttpClient.applicationJsonMediaType, body))
        .protocol(Protocol.HTTP_1_1)
        .request(request)
        .message("Message")
        .build()
    }

    def theCallBack(callback: Callback, request: Request): Unit
    def body: String

    val httpClient = mock[OkHttpClient]
    val call = mock[Call]
    val captor: ArgumentCaptor[Request] = ArgumentCaptor.forClass(classOf[Request])

    httpClient.newCall(any[Request]()) answers {

      (_: Any) match {
        case (request: Request) => {
          call.enqueue(any[Callback]()) answers {
            (_: Any) match {
              case (callBack: Callback) =>
                callBack.onResponse(
                call,
                buildResponse(request, 200, body.getBytes)
              )

            }
          }
          call
        }
      }

      /*
          httpClient.newCall(captor.capture) answers {
            (_: Any) match {
              case (request: Request) =>
                call.enqueue(any[Callback]()) answers {
                  (_: Any) match {
                    case (callback: Callback) => theCallBack(callback, request)
                  }
                }
                call
            }
          }
      */
    }
  }
}
