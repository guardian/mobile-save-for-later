package com.gu.sfl.lib

import java.time.LocalDateTime

import com.gu.sfl.controller.{SavedArticle, SavedArticles}
import com.gu.sfl.exception.{MaxSavedArticleTransgressionError, SavedArticleMergeError}
import com.gu.sfl.persisitence.SavedArticlesPersistence
import org.mockito.Matchers
import org.scalatest.mockito.MockitoSugar
import org.specs2.mutable.Specification
import org.mockito.Mockito._
import org.scalatest.OneInstancePerTest
import org.specs2.specification.Scope

import scala.util.{Failure, Success}

class ArticleMergeSpecification extends Specification with MockitoSugar  {

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
    val savedArticlesUpdate1 = SavedArticles(version, List(article3))
    val savedArticles2 = SavedArticles(version, List(article1, article2, article3))

    "saves the articles if the user does not currently have any articles saved" in new Setup {
      val responseArticles = Success(Some(savedArticles.advanceVersion))
      when(savedArticlesPersistence.read(userId)).thenReturn(Success(None))
      when(savedArticlesPersistence.write(userId, savedArticles)).thenReturn(responseArticles)
      val saved = savedArticlesMerger.updateWithRetryAndMerge(userId, savedArticles)
      verify(savedArticlesPersistence, times(1)).read(Matchers.eq(userId))
      verify(savedArticlesPersistence, never()).update(Matchers.any[String], Matchers.any[SavedArticles])
      saved shouldEqual (responseArticles)
    }

    "will merge the new list correctly if the user aleady has articles stored and there is no conflict" in new Setup {
      val responseArticles = Success(Some(savedArticles2.advanceVersion))
      when(savedArticlesPersistence.read(userId)).thenReturn(Success(Some(savedArticles)))
      when(savedArticlesPersistence.update(Matchers.eq(userId), Matchers.eq(savedArticles2))).thenReturn(responseArticles)
      val saved = savedArticlesMerger.updateWithRetryAndMerge(userId, savedArticlesUpdate1)
      verify(savedArticlesPersistence, times(1)).read(Matchers.eq(userId))
      verify(savedArticlesPersistence, never()).write(Matchers.any[String], Matchers.any[SavedArticles])
      verify(savedArticlesPersistence, times(1)).update(Matchers.eq(userId), Matchers.eq(savedArticles2))
      saved shouldEqual (responseArticles)
    }

    "If there is a conflict in version try again before merging" in new Setup {
      val responseArticles = Success(Some(savedArticles2.advanceVersion))
      val expectedSavedArticles = savedArticles2.copy(version = "2")
      when(savedArticlesPersistence.read(userId)).thenReturn(Success(Some(savedArticles.copy(version = "2"))))
      when(savedArticlesPersistence.update(Matchers.eq(userId), Matchers.eq(expectedSavedArticles))).thenReturn(responseArticles)
      val saved = savedArticlesMerger.updateWithRetryAndMerge(userId, savedArticlesUpdate1)
      verify(savedArticlesPersistence, times(2)).read(Matchers.eq(userId))
      verify(savedArticlesPersistence, never()).write(Matchers.any[String], Matchers.any[SavedArticles])
      verify(savedArticlesPersistence, times(1)).update(Matchers.eq(userId), Matchers.eq(expectedSavedArticles))
      saved shouldEqual (responseArticles)
    }

    "will attempt tp retry a merge up to three times" in new Setup {
      val responseArticles = Success(Some(savedArticles2.advanceVersion))
      val expectedSavedArticles = savedArticles2.copy(version = "3")
      when(savedArticlesPersistence.read(userId)).thenReturn(Success(Some(savedArticles.copy(version = "2")))).thenReturn(Success(Some(savedArticles.copy(version = "3"))))
      //when(savedArticlesPersistence.read(userId)).thenReturn(Success(Some(savedArticles.copy(version = "3"))))
      when(savedArticlesPersistence.update(Matchers.eq(userId), Matchers.eq(expectedSavedArticles))).thenReturn(responseArticles)
      val saved = savedArticlesMerger.updateWithRetryAndMerge(userId, savedArticlesUpdate1)
      verify(savedArticlesPersistence, times(3)).read(Matchers.eq(userId))
      verify(savedArticlesPersistence, never()).write(Matchers.any[String], Matchers.any[SavedArticles])
      verify(savedArticlesPersistence, times(1)).update(Matchers.eq(userId), Matchers.eq(expectedSavedArticles))
      saved shouldEqual (responseArticles)
    }

    "will not attempt to merge saved articles more than three times " in new Setup {
      val responseArticles = Success(Some(savedArticles2.advanceVersion))
      val expectedSavedArticles = savedArticles2.copy(version = "3")
      when(savedArticlesPersistence.read(userId))
          .thenReturn(Success(Some(savedArticles.copy(version = "2"))))
          .thenReturn(Success(Some(savedArticles.copy(version = "3"))))
          .thenReturn(Success(Some(savedArticles.copy(version = "4"))))

      val saved = savedArticlesMerger.updateWithRetryAndMerge(userId, savedArticlesUpdate1)
      verify(savedArticlesPersistence, times(3)).read(Matchers.eq(userId))
      verify(savedArticlesPersistence, never()).write(Matchers.any[String], Matchers.any[SavedArticles])
      verify(savedArticlesPersistence, times(0)).update(Matchers.eq(userId), Matchers.eq(expectedSavedArticles))
      saved shouldEqual (Failure(SavedArticleMergeError("Conflicting version in savedArticles")))
    }

    "will not try to merge a list of articles with a lenght greater than the saved article limit" in new Setup {
      private val articleSaveLimit = 2
      override val savedArticlesMerger = new SavedArticlesMergerImpl(SavedArticlesMergerConfig(articleSaveLimit), savedArticlesPersistence)
      val saved = savedArticlesMerger.updateWithRetryAndMerge(userId, savedArticles2)
      verifyZeroInteractions(savedArticlesPersistence)
      saved mustEqual(Failure(MaxSavedArticleTransgressionError(s"Tried to save more than $articleSaveLimit articles.")))
    }

    "failure to get current articles throws the correct exceptiob" in new Setup {
      when(savedArticlesPersistence.read(userId)).thenReturn(Failure(new IllegalStateException("Bad, bad, bad")))
      val saved = savedArticlesMerger.updateWithRetryAndMerge(userId, savedArticles)
      verify(savedArticlesPersistence, times(1)).read(Matchers.eq(userId))
      saved shouldEqual (Failure(SavedArticleMergeError("Could not retrieve current articles")))
    }

    "failure to update the saved articles results in the currect error" in new Setup {
      when(savedArticlesPersistence.read(userId)).thenReturn(Success(None))
      when(savedArticlesPersistence.write(userId, savedArticles)).thenReturn(Failure(new IllegalStateException("My mummy told me to be good, but I was naughty")))

      val saved = savedArticlesMerger.updateWithRetryAndMerge(userId, savedArticles)
      saved shouldEqual (Failure(SavedArticleMergeError("Could not update articles")))
    }
  }

  trait Setup extends Scope {
    val savedArticlesPersistence = mock[SavedArticlesPersistence]
    val savedArticlesMerger = new SavedArticlesMergerImpl(SavedArticlesMergerConfig(2), savedArticlesPersistence)
  }

}
