import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

case class Saved(id: String, articles: List[String] = List.empty)

def goodId(id: String) : Future[String] = Future.successful(id)
def badId(id: String): Future[String] = Future.failed(new IllegalStateException("No Id"))

def goodArtcles(id: String): Try[Option[Saved]] = Success(Some(Saved(id, List("jezzaisacommie","bestconservatory"))))
def getArtcles(id: String): Try[Option[Saved]] = Success(None)
def badArtcles(id: String): Try[Option[Saved]] = Failure(new IllegalArgumentException("Missing id"))

def futureIseOption(f: Future[Try[Option[Saved]]]): Future[Option[Saved]] =
  f.transformWith {
    case Success(Success(Some(saved))) => Future.successful(Some(saved))
    case _ => Future.successful(None)
  }

def articlesResponse(id: String)
                    (userId: String => Future[String], articles: String => Try[Option[Saved]]): Future[Option[Saved]] = {

  userId(id).transformWith {
    case Success(id) => Future.fromTry(articles(id))
    case Failure(_) => Future.failed(new IllegalStateException("badoy"))
  }
}

def getGoodUser: Option[String] = Some("1234")
def getBadUser: Option[String] = None

def transformArticles(maybeArticles: Future[Option[Saved]]) : Future[FakeLambdaResponse]  =
  maybeArticles.transformWith {
    case Success(Some(saved)) => Future.successful(FakeLambdaResponse(saved.toString))
    case Failure(_) => Future.successful(FakeLambdaResponse("BIG BLACK BOOOOTIEAD"))
  }



case class FakeLambdaResponse(body: String)

//val arts = articlesResponse("1234")(goodId, goodArtcles)

val t = (for {
  userId <- getBadUser
  maybeArticles = articlesResponse(userId)(goodId, goodArtcles)
} yield { transformArticles(maybeArticles) }).getOrElse(Future.successful(FakeLambdaResponse("Dalston junction cocksucker")))

val res = Await.result(t, 4 seconds)

