import org.apache.http.impl.client.FutureRequestExecutionMetrics

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class Saved(id: String, articles: List[String] = List.empty)

def goodId(id: String) : Future[Option[String]] = Future.successful(Some(id))
def noId(id: String) : Future[Option[String]] = Future.successful(None)
def badId(id: String): Future[Option[String]] = Future.failed(new IllegalStateException("No Id"))

def saved(id: String) = Saved(id, List("jezzaisacommie", "bestconservatory"))
def goodArtcles(id: String): Try[Option[Saved]] = Success(Some(saved(id)))
def getArtcles(id: String): Try[Option[Saved]] = Success(None)
def badArtcles(id: String): Try[Option[Saved]] = Failure(new IllegalArgumentException("Missing id"))

def parseGood(json: String) = Success(saved("1234"))

