import com.gu.sfl.lambda.LambdaResponse

import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import com.gu.sfl.lib.Jackson._
import scala.concurrent.duration._

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

case class Saved(id: String, arts: List[String] = List.empty)
case class Response(value: String)

val promiseResponse: Promise[Response] = Promise[Response]
val futureResponse: Future[Response] = promiseResponse.future

val saved = Saved("1234", List("buy-conservatory", "kill-corbyn", "global-warming-bad","best-beach-in-bali"))

val r = Response("1234")

def good(id: String) : Future[Option[Saved]] = {
  //Thread.sleep(1000)
  Future{ Some(saved) }
}

val futureRes = good("1234").transformWith {
  case Success(Some(s)) => Future{ Response(mapper.writeValueAsString(s)) }
  case Success(None) => Future { Response("SoMuchBlather")}
  case Failure(_) => Future { Response("Utter Bs") }
}

futureRes.transform((triedResponse: Try[Response]) => {
  promiseResponse.complete(triedResponse)
  triedResponse
})


Await.ready(futureResponse, Duration(4, SECONDS))

val end = futureResponse.value.getOrElse(Failure(new IllegalStateException("Mutha!")))

end match {
  case Success(response) => response
  case Failure(t) => {
     println("bibbledu-bob")
    Response("Yosa")
  }
}





