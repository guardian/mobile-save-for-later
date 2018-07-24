package com.gu.sfl.persistance

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
    logger.info(s"Attempting to retrieve saved articles for user $userId")
    exec(client)(table.get('userId -> userId)) match {
      case Some(Right(sa)) =>
        logger.debug(s"Retrieved articles for: $userId")
        Success(Some(sa))
      case Some(Left(error)) =>
        val ex = new IllegalArgumentException(s"$error")
        logger.debug(s"Error retrieving articles", ex)
        Failure(ex)
      case None =>
        logger.error("No articles found for user")
        Success(None)
    }
  }

  override def write(userId: String, savedArticles: SavedArticles): Try[Option[SavedArticles]] = {
    logger.info(s"Saving articles with userId $userId")
    exec(client)(table.put(DynamoSavedArticles(userId, savedArticles))) match {
      case Some(Right(articles)) =>
        logger.debug("Succcesfully saved articles")
        Success(Some(articles.ordered))
      case Some(Left(error)) =>
        val exception = new IllegalArgumentException(s"$error")
        logger.debug(s"Exception Thrown saving articles:", exception)
        Failure(exception)
      case None => {
        logger.debug("Successfully saved but none retrieved")
        Success(Some(savedArticles))
      }
    }
  }

  override def update(userId: String, savedArticles: SavedArticles): Try[Option[SavedArticles]] = {
    logger.info(s"Updating saved articles for ${userId}")
    exec(client)(table.update('userId -> userId,
      set('version -> savedArticles.nextVersion) and
        set('articles -> mapper.writeValueAsString(savedArticles.articles)))
    ) match {
      case Right(articles) =>
        logger.debug("Updated articles")
        Success(Some(articles.ordered))
      case Left(error) =>
        val ex = new IllegalStateException(s"${error}")
        logger.error(s"Error updating articles for userId ${userId}: ${ex.getMessage} ")
        Failure(ex)
    }
  }
}

