package com.gu.sfl.model

import java.time.{Instant, LocalDateTime, ZoneOffset}

import com.fasterxml.jackson.annotation.JsonIgnore

object SavedArticle {
  implicit val localDateOrdering: Ordering[LocalDateTime] = Ordering.by(_.toEpochSecond(ZoneOffset.UTC))
  implicit val ordering: Ordering[SavedArticle] = Ordering.by[SavedArticle, LocalDateTime](_.date)
}
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
  def ordered: SavedArticles = copy()
}
