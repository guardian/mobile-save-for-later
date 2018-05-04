package com.gu.sfl.lib

import com.gu.sfl.Parallelism
import okhttp3._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.specs2.mutable.Specification
import org.mockito.Matchers
import org.specs2.specification.Scope

import scala.concurrent.ExecutionContext



class IdentityServiceSpec extends Specification with MockitoSugar {

  implicit val executionContext: ExecutionContext = Parallelism.largeGlobalExecutionContext


  trait MockGoodIdResponseScope extends MockHttpRequestScope {
    override def theCallBack(callback: Callback): Unit = {
       callback.onResponse(
         call,
         buildResponse()
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

    def theCallBack(callback: Callback) : Unit

    val httpClient = mock[OkHttpClient]
    val call = mock[Call]

    when(httpClient.newCall(Matchers.any[Request])) thenAnswer {
      (_: Any) match {
        case (request: Request) => {
          when(call.enqueue(Matchers.any[Callback])) thenAnswer {
            (_: Any) match {
              case (callback: Callback)  =>  {
                theCallBack(callback)
              }
            }
          }
          call
      }
    }
  }



  "IdentityService should"

}
