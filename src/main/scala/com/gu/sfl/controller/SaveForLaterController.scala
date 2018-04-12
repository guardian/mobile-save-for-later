package com.gu.sfl.controller

import java.time._

import com.gu.sfl.Logging
import com.gu.sfl.lambda.{LambdaRequest, LambdaResponse}
import com.gu.sfl.lib.Base64Utils
import com.gu.sfl.util.StatusCodes
import com.gu.sfl.lib.Jackson._
import com.gu.sfl.persisitence.SavedArticlesPersistence

import scala.math.Ordering.Implicits._
import scala.util.{Failure, Success, Try}


object SavedArticle {
  implicit val localDateOrdering: Ordering[LocalDateTime] = Ordering.by(_.toEpochSecond(ZoneOffset.UTC))
  implicit val ordering = Ordering.by[SavedArticle, LocalDateTime](_.date)
}
//TODO - check whether we need read or platform
case class SavedArticle(id: String, shortUrl: String, date: LocalDateTime, read: Boolean)

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

object SavedArticles {
  def nextVersion() = Instant.now().toEpochMilli.toString
  def apply(articles: List[SavedArticle]) : SavedArticles = SavedArticles(nextVersion(), articles)
}

case class SavedArticles(version: String, articles: List[SavedArticle]) extends SyncedPrefsData {
  override def advanceVersion: SyncedPrefsData = copy(version = nextVersion)
  def ordered: SavedArticles = copy()
}



//TODO inject object the reads/writes to dynamo
class SaveForLaterControllerImpl(savedArticlesPersistence: SavedArticlesPersistence) extends Function[LambdaRequest, LambdaResponse] with Base64Utils with Logging {
  override def apply(lambdaRequest: LambdaRequest): LambdaResponse = {
    logger.info("SaveForLaterController - handleReques")
    lambdaRequest match {
      case LambdaRequest(Some(Left(json)), _) =>
        logger.info("Save json as string")
        save(Try(mapper.readValue(json, classOf[SavedArticles])))

      case LambdaRequest(Some(Right(bytes)), _) =>
        logger.info("Save json as bytes")
        save(Try(mapper.readValue(bytes, classOf[SavedArticles])))

      case LambdaRequest(None, _) =>
        logger.info("SaveForLaterController - bad request")
        LambdaResponse(StatusCodes.badRequest, Some(Left("Expected a json body")))
    }
  }

  private def save(triedRequest: Try[SavedArticles]) = {
    logger.info("Trying to save articles")

    val x = triedRequest.map(
      articles =>
        savedArticlesPersistence.write("1234", articles)
          .flatMap(maybeArticles => Try(maybeArticles.get))
            .map(res => LambdaResponse(StatusCodes.ok, Some(Left(mapper.writeValueAsString(res)))))
          .getOrElse(LambdaResponse(StatusCodes.badRequest, Some(Left("Could not unmarshal json"))))
    ).fold( t => {
      logger.info(s"Error saving articles: ${t.getMessage}")
      LambdaResponse(StatusCodes.internalServerError, Some(Left("Could not parse request")))
    }, x => x)
    x
  }
}
