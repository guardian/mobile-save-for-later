package com.gu.sfl.save

import java.time.{Instant, LocalDate}

import com.gu.sfl.Logging
import com.gu.sfl.lambda.{LambdaRequest, LambdaResponse}
import com.gu.sfl.lib.Base64Utils
import com.gu.sfl.util.StatusCodes
import com.gu.sfl.lib.Jackson._

import scala.util.{Failure, Success, Try}

//This is cribbed from the current identity model:  https://github.com/guardian/identity/blob/master/identity-model/src/main/scala/com/gu/identity/model/Model.scala
//Todo - eventually we may no longer need the syncedPrefs hierarchy  because at this point its only saving articles which we're interested in
case class SyncedPrefs(userId: String, savedArticles :Option[SavedArticles])  {
  def ordered: SyncedPrefs = copy( savedArticles = savedArticles.map(_.ordered) )
}

sealed trait SyncedPrefsData {
  def version: String
  val nextVersion = SavedArticles.nextVersion()
  def advanceVersion: SyncedPrefsData

}

case class SavedArticles(version: String, savedArticles: List[SavedArticle]) extends SyncedPrefsData {
  override def advanceVersion: SyncedPrefsData = copy(version = nextVersion)
  def ordered: SavedArticles = copy()
}

object SavedArticles {
  def nextVersion() = Instant.now().toEpochMilli.toString
  def apply(articles: List[SavedArticle]) : SavedArticles = SavedArticles(nextVersion(), articles)
}

//TODO - check whether we need read or platform
case class SavedArticle(id: String, shortUrl: String, date: LocalDate, read: Boolean, platform: Option[String])

object SavedArticle {
  implicit val ordering = Ordering.by[SavedArticle, LocalDate](_.date)
}

trait SaveForLaterController {
  def save(lambdaRequest: LambdaRequest) : LambdaResponse
}



//TODO inject object the reads/writes to dynamo
class SaveForLaterControllerImpl extends SaveForLaterController with Base64Utils with Logging {
  override def save(lambdaRequest: LambdaRequest): LambdaResponse = lambdaRequest match {
    case LambdaRequest(Some(Left(json)), _) => save(Try(mapper.readValue(json, classOf[SavedArticles])) recoverWith {
      case t: Throwable => logger.warn(s"Error readig json: $json")
      Failure(t)
    })

    case LambdaRequest(Some(Right(bytes)), _)=> save(Try(mapper.readValue(bytes, classOf[SavedArticles])) recoverWith {
      case t: Throwable => logger.warn(s"Errof reading json as bytes: ${encoder.encode(bytes)}", t)
      Failure(t)
    })

    case LambdaRequest(None, _) => LambdaResponse(StatusCodes.badRequest, Some(Left("Expected a json body")))

      

  }

  private def save(triedRequest: Try[SavedArticles]) = {
    triedRequest match {
      case Success(savedArticles) => LambdaResponse(StatusCodes.ok, Some(Left(mapper.writeValueAsString(savedArticles))))
      case Failure(_) => LambdaResponse(StatusCodes.badRequest, Some(Left("Could not unmarshal json"))) 
    }
  }
}
