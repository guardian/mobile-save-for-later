package com.gu.sfl.lib

import java.time.LocalDateTime

import com.gu.sfl.controller.{SavedArticle, SavedArticles}
import com.gu.sfl.persisitence.SavedArticlesPersistence
import org.mockito.Matchers
import org.scalatest.mockito.MockitoSugar
import org.specs2.mutable.Specification
import org.mockito.Mockito._

import scala.util.Success

class ArticleMergeSpecification extends Specification with MockitoSugar {

  val savedArticlesPersistance = mock[SavedArticlesPersistence]
  val savedArticlesMerger = new SavedArticlesMergerImpl(savedArticlesPersistance, SavedArticlesMergerConfig(200))

  "updateSavedArticlesWithRetryAndMerge" should {
    val userId = "123"
    val version = "1"

    val (article1, article2, article3, article4) = (
      SavedArticle("id/1", "p/1", LocalDateTime.of(2018, 1, 16, 16, 30), read = true),
      SavedArticle("id/2", "p/2", LocalDateTime.of(2018, 2, 17, 17, 30), read = false),
      SavedArticle("id/3", "p/3", LocalDateTime.of(2018, 3, 18, 18, 30), read = true),
      SavedArticle("id/4", "p/4", LocalDateTime.of(2018, 4, 19, 19, 30), read = true)
    )

    val savedArticles = SavedArticles(version, List(article1, article2))
    val savedArticles2 = SavedArticles(version, List(article1, article2, article3))

    "saves the articles if the user does not currently have any articles saved" in {
      val responseArticles = savedArticles.advanceVersion
      when(savedArticlesPersistance.read(userId)).thenReturn(Success(None))
      when(savedArticlesPersistance.write(userId, savedArticles)).thenReturn(Success(Some(savedArticles)))
      val saved = savedArticlesMerger.updateWithRetryAndMerge(userId, savedArticles)
      verify(savedArticlesPersistance, times(1)).read(Matchers.eq(userId))
      saved shouldEqual(Success(Some(responseArticles)))
    }

    "will merge the new list correctly if the user aleady has articles stored" in {
       val responseArticles = savedArticles2.advanceVersion



   }
  }
}
