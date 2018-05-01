import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import com.gu.sfl.lib.Jackson._

import scala.concurrent.duration.Duration
import scala.util.{Success, Try}

case class Saved(id: String, arts: List[String] = List.empty)
case class Response(value: String)

val promiseResponse: Promise[Response] = Promise[Response]
val futureResponse: Future[Response] = promiseResponse.future

val saved = Saved("1234", List("buy-conservatory", "kill-corbyn", "global-warming-bad","best-beach-in-bali"))

val r = Response("1234")

def good(id: String) : Future[Option[Saved]] = {
  Future{ Some(saved) }
}

def f: Future[String] = {
  Thread.sleep(4300)
  Future {"Hello"}
}

val h = Await.result(f, Duration.Inf)
println(s"H: $h")
println("Wotcha!")


/*good("1234").transform((trySaved: Try[Option[Saved]]) => {

}
)*/





