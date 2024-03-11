package com.gu.sfl.persistence

import org.scanamo.{
  DynamoFormat,
  DynamoReadError,
  DynamoValue,
  MissingProperty,
  PutReturn,
  Scanamo,
  Table
}
import com.gu.sfl.Logging
import com.gu.sfl.lib.Jackson._
import com.gu.sfl.model._
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

case class PersistenceConfig(app: String, stage: String) {
  val tableName = s"$app-$stage-articles"
}

trait SavedArticlesPersistence {
  def read(userId: String): Try[Option[SavedArticles]]

  def update(
      userId: String,
      savedArticles: SavedArticles
  ): Try[Option[SavedArticles]]

  def write(
      userId: String,
      savedArticles: SavedArticles
  ): Try[Option[SavedArticles]]
}

case class DynamoSavedArticles(
    userId: String,
    version: String,
    articles: String
)

object DynamoSavedArticles {
  implicit val formatcom
      : DynamoFormat[com.gu.sfl.persistence.DynamoSavedArticles] =
    new DynamoFormat[DynamoSavedArticles] {
      override def read(
          dv: DynamoValue
      ): Either[DynamoReadError, DynamoSavedArticles] = {
        val av = dv.toAttributeValue
        (for {
          attrs <- Option(av.m()).map(_.asScala)
          userId <- attrs.get("userId").flatMap(a => Option(a.s()))
          version <- attrs.get("version").flatMap(a => Option(a.s()))
          articles <- attrs.get("articles").flatMap(a => Option(a.s()))
        } yield {
          DynamoSavedArticles(userId, version, articles)
        }).fold(
          Left(MissingProperty): Either[DynamoReadError, DynamoSavedArticles]
        )(s => Right(s))
      }

      override def write(t: DynamoSavedArticles): DynamoValue = {
        val av = AttributeValue
          .builder()
          .m(
            Map(
              "userId" -> AttributeValue.builder().s(t.userId).build(),
              "version" -> AttributeValue.builder().s(t.version).build(),
              "articles" -> AttributeValue.builder().s(t.articles).build()
            ).asJava
          )
          .build()
        DynamoValue.fromAttributeValue(av)
      }
    }

  def apply(userId: String, savedArticles: SavedArticles): DynamoSavedArticles =
    DynamoSavedArticles(
      userId,
      savedArticles.nextVersion,
      mapper.writeValueAsString(savedArticles.articles)
    )
}

class SavedArticlesPersistenceImpl(persistanceConfig: PersistenceConfig)
    extends SavedArticlesPersistence
    with Logging {
  implicit def toSavedArticles(
      dynamoSavedArticles: DynamoSavedArticles
  ): SavedArticles = {
    val articles =
      mapper.readValue[List[SavedArticle]](dynamoSavedArticles.articles)
    SavedArticles(dynamoSavedArticles.version, articles)
  }

  private val client = DynamoDbClient
    .builder()
    .credentialsProvider(DefaultCredentialsProvider.create())
    .build()
  //TODO confirm that it's ok to share the same client concurrently in all requests.. I guess if this is a lambda there won't be concurrent requests anyway ?
  private val scanamo = Scanamo(client)

  import org.scanamo.syntax._

  val table = Table[DynamoSavedArticles](persistanceConfig.tableName)

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

  override def write(
      userId: String,
      savedArticles: SavedArticles
  ): Try[Option[SavedArticles]] = {
    scanamo.exec(
      table.putAndReturn(PutReturn.NewValue)(
        DynamoSavedArticles(userId, savedArticles)
      )
    ) match {
      case Some(Right(articles)) =>
        logger.debug(s"Succcesfully saved articles for $userId")
        Success(Some(articles.ordered))
      case Some(Left(error)) =>
        val exception = new IllegalArgumentException(s"$error")
        logger.debug(
          s"Exception Thrown saving articles for $userId:",
          exception
        )
        Failure(exception)
      case None => {
        logger.debug(s"Successfully saved but none retrieved for $userId")
        Success(Some(savedArticles))
      }
    }
  }

  override def update(
      userId: String,
      savedArticles: SavedArticles
  ): Try[Option[SavedArticles]] = {
    scanamo.exec(
      table.update(
        "userId" -> userId,
        set("version" -> savedArticles.nextVersion) and
          set("articles" -> mapper.writeValueAsString(savedArticles.articles))
      )
    ) match {
      case Right(articles) =>
        logger.debug("Updated articles")
        Success(Some(articles.ordered))
      case Left(error) =>
        val ex = new IllegalStateException(s"${error}")
        logger.error(
          s"Error updating articles for userId ${userId}: ${ex.getMessage} "
        )
        Failure(ex)
    }
  }
}
