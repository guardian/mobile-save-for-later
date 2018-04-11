package com.gu.sfl.persisitence

import java.time.LocalDateTime

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDBAsync, AmazonDynamoDBAsyncClient}
import com.gu.sfl.Logging
import com.gu.sfl.save.SavedArticles
import com.gu.scanamo.Scanamo.exec
import com.gu.scanamo.Table
import com.gu.scanamo.syntax._


import scala.util.{Success, Try}

case class DynamoSavedArticle(id: String, version: String, shortUrl: String, date: LocalDateTime, read: Boolean)

trait SavedArticlesPersistence {
  def read(userId: String) : Try[Option[List[DynamoSavedArticle]]]

  def write(savedArticles: SavedArticles) : Try[Option[Int]]
}

class SavedArticledPersistenceImpl(dynamoTableName: String) extends SavedArticlesPersistence with Logging {

  private val client: AmazonDynamoDBAsync = AmazonDynamoDBAsyncClient.asyncBuilder().withCredentials(DefaultAWSCredentialsProviderChain.getInstance()).build()
  private val table = Table[DynamoSavedArticle](dynamoTableName)

  private def savedArticlesToDynamo(savedArticles: SavedArticles) : Set[DynamoSavedArticle] = {
    savedArticles.savedArticles.map{ a => DynamoSavedArticle(a.id, savedArticles.version, a.shortUrl, a.date, a.read)}.toSet
  }

  override def read(userId: String): Try[Option[List[DynamoSavedArticle]]] = Success(Some(List.empty))

  override def write(savedArticles: SavedArticles): Try[Option[Int] = {
    val articles = savedArticles.savedArticles.map{ a => DynamoSavedArticle(a.id, savedArticles.version, a.shortUrl, a.date, a.read)}.toSet

//    def writeBatch(articles: Set[DynamoSavedArticle])

    val results = exec(client)(table.putAll(articles) )
    val f = results.flatMap{
      r => Option(r) map(_.getUnprocessedItems)
    }.flatMap{ t => t

  }
}

