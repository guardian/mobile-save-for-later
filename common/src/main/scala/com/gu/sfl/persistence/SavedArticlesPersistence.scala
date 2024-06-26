package com.gu.sfl.persistence

import org.scanamo.{Scanamo, Table}
import org.scanamo.syntax._
import com.gu.sfl.Logging
import com.gu.sfl.lib.Jackson._
import com.gu.sfl.model._
import org.scanamo.generic.auto.genericDerivedFormat
import software.amazon.awssdk.auth.credentials.{AwsCredentialsProviderChain, EnvironmentVariableCredentialsProvider, ProfileCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

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

}

class SavedArticlesPersistenceImpl(persistanceConfig: PersistenceConfig) extends SavedArticlesPersistence with Logging {
  implicit def toSavedArticles(dynamoSavedArticles: DynamoSavedArticles): SavedArticles = {
    val articles = mapper.readValue[List[SavedArticle]](dynamoSavedArticles.articles)
    SavedArticles(dynamoSavedArticles.version, articles)
  }
  private val client = DynamoDbClient
                        .builder()
                        .credentialsProvider(
                            AwsCredentialsProviderChain.of(EnvironmentVariableCredentialsProvider.create(),
                              ProfileCredentialsProvider.create("mobile"))
                        ).region(Region.EU_WEST_1)
                        .build()
  //TODO confirm that it's ok to share the same client concurrently in all requests.. I guess if this is a lambda there won't be concurrent requests anyway ?
  private val scanamo = Scanamo(client)
  private val table = Table[DynamoSavedArticles](persistanceConfig.tableName)

  override def read(userId: String): Try[Option[SavedArticles]] = {
    scanamo.exec(table.get("userId" -> userId)) match {
      case Some(Right(sa)) =>
        logger.debug(s"Retrieved articles for: $userId")
        Success(Some(sa))
      case Some(Left(error)) =>
        val ex = new IllegalArgumentException(s"$error")
        logger.debug(s"Error retrieving articles for $userId", ex)
        Failure(ex)
      case None =>
        logger.error(s"No articles found for user $userId")
        Success(None)
    }
  }

  /** *
   * Using the update method as a create or update method, because
   * dynamo update allows us to return newly created values:
   * https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_UpdateItem.html
   * Previously we had been using the put method to create new records,
   * however the put operation only supports return values of the old value or none
   * https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_PutItem.html
   * Since we use the database response to respond to the client, we want to have
   * the latest data from the data
   * @param userId
   * @param savedArticles
   * @return savedArticles
   *
   *         * */

  override def update(userId: String, savedArticles: SavedArticles): Try[Option[SavedArticles]] = {
    scanamo.exec(table.update("userId" -> userId,
        set("version" -> savedArticles.nextVersion) and
        set("articles" -> mapper.writeValueAsString(savedArticles.articles)))
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
