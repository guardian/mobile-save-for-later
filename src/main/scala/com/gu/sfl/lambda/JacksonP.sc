import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Try}

case class Articles(id: String, articles: List[String] = List.empty )
val articles = Articles("1234", List("corbyn's a toosser", "glaciers are collapsing", "designer conservatory - best buy"))


