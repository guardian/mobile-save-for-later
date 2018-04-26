import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Try}

case class Articles(id: String, articles: List[String] = List.empty )



def getId(token: String): Future[Option[String]] ={
  Thread.sleep(1000)
  Future.successful(Some("123"))
}

def getFakeArticles(id: String): Try[Option[Articles]] = Success(Some(Articles(id, List("art"))))

val articles = getId("id").map {
 maybeId => maybeId.map {
   id => getFakeArticles(id)
 }.getOrElse(Success(Articles("id")))
}



val x = Await.result(articles, 2 second)

def getTheArticles(id: String)
                  (getId: String => Future[Option[String]],
                   getArticicles: String => Try[Option[Articles]] ) = {
  for {
    maybeId <- getId(id)

  }
}






