package com.gu.sfl.model

import java.time.LocalDateTime

import org.specs2.mutable.Specification

class SavedArticlesTest extends Specification {
  private val baseDateTime = LocalDateTime.now()
  "DirtySavedArticles -> SavedArticles" should {
    "Empty List" in {
      SavedArticles(DirtySavedArticles("", List())) must beEqualTo(SavedArticles("", List()))
    }
    "Single Good Item" in {
      val id = "id"
      val url = "url"
      val date = baseDateTime
      val read = true
      SavedArticles(DirtySavedArticles("", List(DirtySavedArticle(Some(id), Some(url), Some(date), read)))) must beEqualTo(SavedArticles("", List(SavedArticle(id,url,date,read))))
    }
    "Two Good Items (order retained)" in {
      val id = "id1"
      val url = "url"
      val date = baseDateTime
      val read = true
      val range = Range(0, 10).toList
      val dirtyarticles = range.map(index => DirtySavedArticle(Some(s"$id$index"), Some(s"$url$index"), Some(date.plusDays(index)), read))
      val articles = range.map(index => SavedArticle(s"$id$index", s"$url$index", date.plusDays(index), read))
      SavedArticles(DirtySavedArticles("", dirtyarticles)) must beEqualTo(SavedArticles("", articles))
    }

    "First missing date" in {
      val id = "id1"
      val url = "url"
      val date = baseDateTime
      val read = true
      val dirtyarticles = List(DirtySavedArticle(Some(s"${id}1"), Some(s"${url}1"), None, read),DirtySavedArticle(Some(s"${id}2"), Some(s"${url}2"), Some(baseDateTime), read))
      val articles = List(SavedArticle(s"${id}1", s"${url}1", baseDateTime.minusSeconds(1), read),SavedArticle(s"${id}2", s"${url}2", baseDateTime, read))
      SavedArticles(DirtySavedArticles("", dirtyarticles)) must beEqualTo(SavedArticles("", articles))
    }

    "Last missing date" in {
      val id = "id1"
      val url = "url"
      val date = baseDateTime
      val read = true
      val dirtyarticles = List(DirtySavedArticle(Some(s"${id}1"), Some(s"${url}1"), Some(baseDateTime), read),DirtySavedArticle(Some(s"${id}2"), Some(s"${url}2"), None, read))
      val articles = List(SavedArticle(s"${id}1", s"${url}1", baseDateTime, read),SavedArticle(s"${id}2", s"${url}2", baseDateTime.plusSeconds(1), read))
      SavedArticles(DirtySavedArticles("", dirtyarticles)) must beEqualTo(SavedArticles("", articles))
    }


    "All missing dates" in {
      val oldDate = LocalDateTime.of(2010,1,1,0,0,0)
      val id = "id1"
      val url = "url"
      val read = true
      val dirtyarticles = List(DirtySavedArticle(Some(s"${id}1"), Some(s"${url}1"), None, read),DirtySavedArticle(Some(s"${id}2"), Some(s"${url}2"), None, read))
      val articles = List(SavedArticle(s"${id}1", s"${url}1", oldDate.plusSeconds(1), read),SavedArticle(s"${id}2", s"${url}2", oldDate.plusSeconds(2), read))
      SavedArticles(DirtySavedArticles("", dirtyarticles)) must beEqualTo(SavedArticles("", articles))
    }
  }
}
