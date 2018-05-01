import scala.concurrent.{Future, Promise}
import com.gu.sfl.lib.Jackson._
import scala.util.Success

case class Saved(id: String, arts: List[String] = List.empty)
case class Response(value: String)

val promiseResponse: Promise[Response] = Promise[Response]
val futureResponse: Future[Response] = promiseResponse.future

val saved = Saved("1234", List("buy-conservatory", "kill-corbyn", "global-warming-bad","best-beach-in-bali"))

val r = Response("1234")

def good(id: String) : Future[Option[Saved]] = {
  Future{ Some(saved) }
}






