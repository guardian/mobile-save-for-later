import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

case class Saved(id: String, articles: List[String] = List.empty)

def goodId(id: String) : Future[Option[String]] = Future.successful(Some(id))
def noId(id: String) : Future[Option[String]] = Future.successful(None)
def badId(id: String): Future[Option[String]] = Future.failed(new IllegalStateException("No Id"))

def goodArtcles(id: String): Try[Option[Saved]] = Success(Some(Saved(id, List("jezzaisacommie","bestconservatory"))))
def getArtcles(id: String): Try[Option[Saved]] = Success(None)
def badArtcles(id: String): Try[Option[Saved]] = Failure(new IllegalArgumentException("Missing id"))

//def triedArtclesToFuture(triedArticles: Try[Option[Saved]]) : Future[Option[Saved]]

def articlesResponse(userId: String)(user: String => Future[Option[String]], articles: String => Try[Option[Saved]]): Future[Option[Saved]] = {
  user(userId).transformWith {
    case Success(Some(id)) => Future.fromTry(articles(id))
    case Success(None) => Future.successful(Some(Saved("")))
    case _ => Future.successful(None)
  }
}

case class FakeLambdaResponse(body: String)

val arts = articlesResponse("1234")(goodId, goodArtcles)

val tes = arts.transformWith {
  case Success(Some(saved)) => Future.successful(FakeLambdaResponse(saved.toString))
  case Success(None) => Future.successful(FakeLambdaResponse("noId"))
  case Failure(_) => Future.successful(FakeLambdaResponse("black bootie"))
}


val ultimate = Await.result(tes, 1 minute)



