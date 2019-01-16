package com.gu.sfl.persistence

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDBAsync, AmazonDynamoDBAsyncClient}
import com.gu.scanamo.Scanamo.exec
import com.gu.scanamo.Table
import com.gu.scanamo.syntax.{set, _}
import com.gu.sfl.Logging
import com.gu.sfl.lib.CloudWatchMetrics
import com.gu.sfl.lib.Jackson._
import com.gu.sfl.model._

import scala.util.{Failure, Success, Try}

case class PersistenceConfig(app: String, stage: String) {
  val tableName = s"$app-$stage-articles"
}

case class DynamoSavedArticles(userId: String, version: String, articles: String)

object DynamoSavedArticles  {
  def apply(userId: String, savedArticles: SavedArticles): DynamoSavedArticles = DynamoSavedArticles(userId, savedArticles.nextVersion, mapper.writeValueAsString(savedArticles.articles))
}

trait SavedArticlesPersistence {
  def read(userId: String) : Try[Option[SavedArticles]]

  def update(userId: String, savedArticles: SavedArticles): Try[Option[SavedArticles]]

  def write(userId: String, savedArticles: SavedArticles) : Try[Option[SavedArticles]]
}

class SavedArticlesPersistenceImpl(persistanceConfig: PersistenceConfig, cloudWatchMetrics: CloudWatchMetrics) extends SavedArticlesPersistence with Logging {
  implicit def toSavedArticles(dynamoSavedArticles: DynamoSavedArticles): SavedArticles = {
    val articles = mapper.readValue[List[SavedArticle]](dynamoSavedArticles.articles)
    SavedArticles(dynamoSavedArticles.version, articles)
  }

  private val client: AmazonDynamoDBAsync = AmazonDynamoDBAsyncClient.asyncBuilder().withCredentials(DefaultAWSCredentialsProviderChain.getInstance()).build()
  private val table = Table[DynamoSavedArticles](persistanceConfig.tableName)

  override def read(userId: String): Try[Option[SavedArticles]] = {
    val timer = cloudWatchMetrics.startTimer("dynamo-read")
    exec(client)(table.get('userId -> userId)) match {
      case Some(Right(sa)) =>
        timer.success
        logger.debug(s"Retrieved articles for: $userId")
        Success(Some(sa))
      case Some(Left(error)) =>
        timer.fail
        val ex = new IllegalArgumentException(s"$error")
        logger.debug(s"Error retrieving articles for $userId", ex)
        Failure(ex)
      case None =>
        timer.state("empty")
        logger.error(s"No articles found for user $userId")
        Success(None)
    }
  }

  override def write(userId: String, savedArticles: SavedArticles): Try[Option[SavedArticles]] = {
    val timer = cloudWatchMetrics.startTimer("dynamo-write")

    exec(client)(table.put(DynamoSavedArticles(userId, savedArticles))) match {
      case Some(Right(articles)) =>
        logger.debug(s"Succcesfully saved articles for $userId")
        timer.success
        Success(Some(articles.ordered))
      case Some(Left(error)) =>
        timer.fail
        val exception = new IllegalArgumentException(s"$error")
        logger.debug(s"Exception Thrown saving articles for $userId:", exception)
        Failure(exception)
      case None => {
        timer.state("empty")
        logger.debug(s"Successfully saved but none retrieved for $userId")
        Success(Some(savedArticles))
      }
    }
  }

  override def update(userId: String, savedArticles: SavedArticles): Try[Option[SavedArticles]] = {
    val timer = cloudWatchMetrics.startTimer("dynamo-update")
    exec(client)(table.update('userId -> userId,
        set('version -> savedArticles.nextVersion) and
        set('articles -> mapper.writeValueAsString(savedArticles.articles)))
    ) match {
      case Right(articles) =>
        timer.success
        logger.debug("Updated articles")
        Success(Some(articles.ordered))
      case Left(error) =>
        timer.fail
        val ex = new IllegalStateException(s"${error}")
        logger.error(s"Error updating articles for userId ${userId}: ${ex.getMessage} ")
        Failure(ex)
    }
  }
}
