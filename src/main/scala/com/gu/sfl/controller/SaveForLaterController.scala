package com.gu.sfl.controller

import java.time._
import java.util.concurrent.TimeUnit

import com.gu.sfl.exception.{MissingAccessTokenException, UserNotFoundException}
import com.gu.sfl.{Logging, Parallelism}
import com.gu.sfl.lambda.{LambdaRequest, LambdaResponse}
import com.gu.sfl.lib.Base64Utils
import com.gu.sfl.util.StatusCodes
import com.gu.sfl.lib.Jackson._
import com.gu.sfl.persisitence.SavedArticlesPersistence
import com.gu.sfl.savedarticles.UpdateSavedArticles

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.math.Ordering.Implicits._
import scala.util.{Failure, Success, Try}


object SavedArticle {
  implicit val localDateOrdering: Ordering[LocalDateTime] = Ordering.by(_.toEpochSecond(ZoneOffset.UTC))
  implicit val ordering = Ordering.by[SavedArticle, LocalDateTime](_.date)
}
//TODO - check whether we need read or platform
case class SavedArticle(id: String, shortUrl: String, date: LocalDateTime, read: Boolean)

case class SyncedPrefsResponse(status: String, syncedPrefs: SyncedPrefs)

//This is cribbed from the current identity model:  https://github.com/guardian/identity/blob/master/identity-model/src/main/scala/com/gu/identity/model/Model.scala
//Todo - eventually we may no longer need the syncedPrefs hierarchy  because at this point its only saving articles which we're interested in
case class SyncedPrefs(userId: String, savedArticles :Option[SavedArticles])  {
  def ordered: SyncedPrefs = copy( savedArticles = savedArticles.map(_.ordered) )
  lazy val size = savedArticles.map(_.articles.size).getOrElse(0) //For dev
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
  override def advanceVersion: SavedArticles = copy(version = nextVersion)
  def ordered: SavedArticles = copy()
}

trait SaveForLaterController {
  val missingUserResponse = LambdaResponse(StatusCodes.badRequest, Some("Could not find a user "))
  val missingAccessTokenResponse = LambdaResponse(StatusCodes.badRequest, Some("could not find an access token"))
  val serverError = LambdaResponse(StatusCodes.internalServerError, Some("Server error"))
  val emptyArticlesResponse = LambdaResponse(StatusCodes.ok, Some(mapper.writeValueAsString(SavedArticles(List.empty))))
  def okSavedArticlesResponse(syncedPrefs: SyncedPrefs): LambdaResponse = LambdaResponse(StatusCodes.ok, Some(mapper.writeValueAsString(SyncedPrefsResponse("ok", syncedPrefs))))
}

//TODO inject object the reads/writes to dynamo
//TODO test for json errors as per  current syncedPrefs logic

class SaveForLaterControllerImpl(updateSavedArticles: UpdateSavedArticles) extends Function[LambdaRequest, Future[LambdaResponse]] with SaveForLaterController with Base64Utils with Logging {

  implicit val executionContext: ExecutionContext = Parallelism.largeGlobalExecutionContext

  override def apply(lambdaRequest: LambdaRequest): Future[LambdaResponse] = {
    logger.info("SaveForLaterController - handleReques")
    val futureRespons = lambdaRequest match {
      case LambdaRequest(Some(json), _, _) =>
        logger.info("Save json as string")
        futureSave(Try(mapper.readValue(json, classOf[SavedArticles])), lambdaRequest.headers)

      case LambdaRequest(None, _,  _) =>
        logger.info("SaveForLaterController - bad request")
        Future { LambdaResponse(StatusCodes.badRequest, Some("Expected a json body")) }
    }
    futureRespons
  }

  private def futureSave(triedRequest: Try[SavedArticles], requestHeaders: Map[String, String] ): Future[LambdaResponse] = {
     (for{
       articlestoSave <- Future.fromTry(triedRequest)
       maybeSyncedPrefs <- updateSavedArticles.saveSavedArticles(requestHeaders, articlestoSave)
     }yield {
       maybeSyncedPrefs
     }).transformWith {
       case Success(Some(syncedPrefs)) =>
         logger.info("Got articles back from db")
         Future { okSavedArticlesResponse(syncedPrefs) }
       case Success(None) =>
          logger.info("No articles found for user")
          Future { emptyArticlesResponse }
       case Failure(t: Throwable) =>
          logger.info(s"Error saving articles: ${t.getMessage}")
          t match {
            case m: MissingAccessTokenException => Future{ missingAccessTokenResponse }
            case u: UserNotFoundException => Future{ missingUserResponse }
            case _ => Future { serverError }
          }
     }
  }

}
