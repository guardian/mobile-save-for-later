package com.gu.sfl.model

import java.io.IOException
import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneOffset}

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.{JsonGenerator, JsonProcessingException}
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer

object SavedArticle {
  implicit val localDateOrdering: Ordering[LocalDateTime] = Ordering.by(_.toEpochSecond(ZoneOffset.UTC))
  implicit val ordering: Ordering[SavedArticle] = Ordering.by[SavedArticle, LocalDateTime](_.date)
}

@JsonSerialize(using = classOf[SavedArticleSerializer])
case class SavedArticle(id: String, shortUrl: String, date: LocalDateTime, read: Boolean)

case class SyncedPrefsResponse(status: String, syncedPrefs: SyncedPrefs)

/*This is cribbed from the current identity model:  https://github.com/guardian/identity/blob/master/identity-model/src/main/scala/com/gu/identity/model/Model.scala
  This service was designed to sync various categories of data for a signed in user of which saved articles were one flavour - hence synced prefs. Because we need to preserve integrity with
  existing clients (ie apps) we need to maintain this model in order to render the same json
*/
case class SyncedPrefs(userId: String, savedArticles :Option[SavedArticles])  {
  def ordered: SyncedPrefs = copy( savedArticles = savedArticles.map(_.ordered) )
}

sealed trait SyncedPrefsData {
  def version: String
  @JsonIgnore
  val nextVersion = SavedArticles.nextVersion()
  def advanceVersion: SyncedPrefsData
}

object SavedArticles {
  def nextVersion() = Instant.now().toEpochMilli.toString
  def apply(articles: List[SavedArticle]) : SavedArticles = SavedArticles(nextVersion(), articles)
}

case class SavedArticles(version: String, articles: List[SavedArticle]) extends SyncedPrefsData {
  override def advanceVersion: SavedArticles = copy(version = nextVersion)
  def ordered: SavedArticles = copy(articles = articles.sorted)
}

object ArticleSerializer {
  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
}

class SavedArticleSerializer(t:Class[SavedArticle]) extends StdSerializer[SavedArticle](t) {


  def this() = this(null)


  @Override
  @throws(classOf[IOException])
  @throws(classOf[JsonProcessingException])
  def serialize(value: SavedArticle, gen: JsonGenerator, serializers: SerializerProvider)
  = {
    gen.writeStartObject()
    gen.writeStringField("id", value.id)
    gen.writeStringField("short", value.shortUrl)
    gen.writeStringField("date", ArticleSerializer.formatter.format(value.date))
    gen.writeBooleanField("read", value.read)
    gen.writeEndObject()
  }
}

