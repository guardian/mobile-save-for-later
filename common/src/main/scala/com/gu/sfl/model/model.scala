package com.gu.sfl.model

import java.io.IOException
import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneOffset}

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser, JsonProcessingException}
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.{DeserializationContext, JsonNode, SerializerProvider}

object SavedArticle {
  implicit val localDateOrdering: Ordering[LocalDateTime] = Ordering.by(_.toEpochSecond(ZoneOffset.UTC))
  implicit val ordering: Ordering[SavedArticle] = Ordering.by[SavedArticle, LocalDateTime](_.date)
}


@JsonSerialize(using = classOf[SavedArticleSerializer])
case class SavedArticle(id: String, shortUrl: String, date: LocalDateTime, read: Boolean)

@JsonDeserialize(using = classOf[DirtySavedArticleDeserializer])
case class DirtySavedArticle(id: Option[String], shortUrl: Option[String], date: Option[LocalDateTime], read: Boolean)

case class SyncedPrefsResponse(status: String, syncedPrefs: SyncedPrefs)

case class SavedArticlesResponse(status: String, savedArticles: SavedArticles)

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
  private val oldDate = LocalDateTime.of(2010,1,1,0,0,0)
  def nextVersion() = Instant.now().toEpochMilli.toString
  def apply(articles: List[SavedArticle]) : SavedArticles = SavedArticles(nextVersion(), articles)
  def apply(dirtySavedArticles: DirtySavedArticles) : SavedArticles = SavedArticles(dirtySavedArticles.version, buildArticlesWithDates(dirtySavedArticles))
  private def buildArticlesWithDates(dirtySavedArticles: DirtySavedArticles) = {
    val startingDate = dirtySavedArticles.articles.flatMap(_.date).headOption.map(_.minusSeconds(2)).getOrElse(oldDate)
    dirtySavedArticles.articles.foldLeft((startingDate, List.empty[SavedArticle])) {
      case ((lastGoodDate, clean), dirtySavedArticle) => {
        dirtySavedArticle match {
          case DirtySavedArticle(Some(id), Some(shortUrl), date, read) => {
            val thisDate = date.getOrElse(lastGoodDate.plusSeconds(1))
            (thisDate, SavedArticle(id, shortUrl, thisDate, read) :: clean)
          }
          case _ => (lastGoodDate, clean)
        }
      }
    }._2.reverse
  }

  val empty = SavedArticles("1", List.empty)
}

case class SavedArticles(version: String, articles: List[SavedArticle]) extends SyncedPrefsData {
  override def advanceVersion: SavedArticles = copy(version = nextVersion)
  @JsonIgnore
  lazy val numberOfArticles = articles.length
  def ordered: SavedArticles = copy(articles = articles.sorted)
  def deduped: SavedArticles = copy( articles = articles.groupBy(_.id).map(_._2.max).toList.sorted )
  def mostRecent(limit: Int) = copy( articles = articles.sorted.takeRight(limit)  )
}

case class DirtySavedArticles(version: String, articles: List[DirtySavedArticle])
case class ErrorResponse(status: String = "error", errors: List[Error])

case class Error(message: String, description: String)

object SavedArticleDateSerializer {
  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
}

class DirtySavedArticleDeserializer(t: Class[DirtySavedArticle]) extends StdDeserializer[DirtySavedArticle](t)  {
  def this () = this(null)
  @Override
  @throws(classOf[IOException])
  @throws(classOf[JsonProcessingException])
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): DirtySavedArticle = {
    val node: JsonNode = p.readValueAsTree()
    val id = Option(node.get("id")).filter(_.isTextual).map(_.asText())
    val shortUrl = Option(node.get("shortUrl")).filter(_.isTextual).map(_.asText())
    val read = Option(node.get("read")).filter(_.isBoolean).map(_.asBoolean())
    val date = Option(node.get("date")).filter(_.isTextual).map(_.asText()).map(LocalDateTime.parse(_, SavedArticleDateSerializer.formatter))
    DirtySavedArticle(id, shortUrl, date, read.getOrElse(false))
  }
}

class SavedArticleSerializer(t:Class[SavedArticle]) extends StdSerializer[SavedArticle](t) {

  def this() = this(null)

  @Override
  @throws(classOf[IOException])
  @throws(classOf[JsonProcessingException])
  def serialize(value: SavedArticle, gen: JsonGenerator, serializers: SerializerProvider) = {
    gen.writeStartObject()
    gen.writeStringField("id", value.id)
    gen.writeStringField("shortUrl", value.shortUrl)
    gen.writeStringField("date", SavedArticleDateSerializer.formatter.format(value.date))
    gen.writeBooleanField("read", value.read)
    gen.writeEndObject()
  }
}

