import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._



import com.gu.sfl.lib.Jackson._

import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration._

def goodTry() : Try[Option[String]] = Success(Some("frenchkiss"))
def badTry() : Try[Option[String]] = Failure(new IllegalStateException("Ooh mot there"))

val good = Future.fromTry(goodTry())
val bad = Future.fromTry(badTry())

val k = good.transformWith {
  case Success(Some(s)) => Future{s}
  case Failure(_) => Future{"buggery"}
}

val res = Await.result(k, 1 minute )
