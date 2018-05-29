package com.gu.sfl.persisitence

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDBAsync, AmazonDynamoDBAsyncClient}
import com.gu.scanamo.Scanamo.exec
import com.gu.scanamo.Table
import com.gu.scanamo.syntax.{set, _}
import com.gu.sfl.Logging
import com.gu.sfl.lib.Jackson._
import com.gu.sfl.model._

import scala.util.{Failure, Success, Try}

case class PersistanceConfig(app: String, stage: String) {
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

class SavedArticlesPersistenceImpl(persistanceConfig: PersistanceConfig) extends SavedArticlesPersistence with Logging {

  implicit def toSavedArticles(dynamoSavedArticles: DynamoSavedArticles): SavedArticles = {
    val articles = mapper.readValue[List[SavedArticle]](dynamoSavedArticles.articles)
    SavedArticles(dynamoSavedArticles.version, articles)
  }

  private val client: AmazonDynamoDBAsync = AmazonDynamoDBAsyncClient.asyncBuilder().withCredentials(DefaultAWSCredentialsProviderChain.getInstance()).build()
  private val table = Table[DynamoSavedArticles](persistanceConfig.tableName)

  override def read(userId: String): Try[Option[SavedArticles]] = {
    logger.info(s"Attempting to retrived saved articles for user $userId")
    exec(client)(table.get('userId -> userId)) match {
      case Some(Right(sa)) =>
        logger.info(s"Retrieved articles for: $userId")
        Success(Some(sa))
      case Some(Left(error)) =>
        val ex = new IllegalArgumentException(s"$error")
        logger.info(s"Error retrieving articles", ex)
        Failure(ex)
      case None =>
        logger.error("No articles found for user")
        Success(None)
    }
  }

  override def write(userId: String, savedArticles: SavedArticles): Try[Option[SavedArticles]] = {
    logger.info(s"Saving articles with userId $userId")
    exec(client)(table.put(DynamoSavedArticles(userId, savedArticles))) match {
      case Some(Right(as)) =>
        logger.info("Succcesfully saved articles")
        Success(Some(as))
      case Some(Left(error)) =>
        val exception = new IllegalArgumentException(s"$error")
        logger.info(s"Exception Thrown saving articles:", exception)
        Failure(exception)
      case None => {
        logger.info("Save an none")
        Success(None)
      }
    }
  }
  
  override def update(userId: String, savedArticles: SavedArticles): Try[Option[SavedArticles]] = {
    logger.info("Updating saved articles four userId")
    exec(client)(table.update('userId -> userId,
      set('version -> savedArticles.nextVersion) and
      set('articles -> mapper.writeValueAsString(savedArticles.articles)))
    ) match {
        case Right(articles) =>
          logger.info("Updated articles")
          Success(Some(articles))
        case Left(error) =>
          val ex = new IllegalStateException(s"${error}")
          logger.info(s"unexpected update outcome: ")
          Failure(ex)
    }
  }
}

