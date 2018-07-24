package com.gu.sfl.lib

import java.time.LocalDateTime

import com.gu.sfl.exception.{MaxSavedArticleTransgressionError, SavedArticleMergeError}
import com.gu.sfl.model.{SavedArticle, SavedArticles, SyncedPrefs}
import com.gu.sfl.persisitence.SavedArticlesPersistenceImpl
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.util.{Failure, Success}

class ArticleMergeSpecification extends Specification with Mockito  {

  "updateSavedArticlesWithRetryAndMerge" should {
    val userId = "123"
    val version = "1"

    val (article1, article2, article3, article4) = (
      SavedArticle("id/1", "p/1", LocalDateTime.of(2018, 4, 16, 16, 30), read = true),
      SavedArticle("id/2", "p/2", LocalDateTime.of(2018, 3, 17, 17, 30), read = false),
      SavedArticle("id/3", "p/3", LocalDateTime.of(2018, 2, 18, 18, 30), read = true),
      SavedArticle("id/4", "p/4", LocalDateTime.of(2018, 1, 19, 19, 30), read = true)
    )


    val savedArticles = SavedArticles(version, List(article1, article2))
    val savedArticlesUpdate1 = SavedArticles(version, List(article3))
    val savedArticles2 = SavedArticles(version, List(article1, article2, article3))
    val article1Dup = SavedArticle("id/1", "p/1", LocalDateTime.of(2017, 1, 16, 16, 30), read = true)
    val article2Dup = SavedArticle("id/2", "p/2", LocalDateTime.of(2017, 3, 17, 17, 30), read = false)


    "saves the articles if the user does not currently have any articles saved" in new Setup {
      val responseArticles = Success(Some(savedArticles.advanceVersion))
      val expectedMergeResponse = Right(savedArticles.advanceVersion)

      savedArticlesPersistence.read(userId) returns (Success(None))
      savedArticlesPersistence.write(userId, savedArticles) returns (responseArticles)
      val saved = savedArticlesMerger.updateWithRetryAndMerge(userId, savedArticles)
      there was one(savedArticlesPersistence).read(userId)

      there was no(savedArticlesPersistence).update(Mockito.any[String](), Mockito.any[SavedArticles]())
      there were no(savedArticlesPersistence).update(userId, savedArticles)
      saved shouldEqual (expectedMergeResponse)
    }

    "will update the the users' saved articles if there is no conflict" in new Setup {
      val responseArticles = Success(Some(savedArticles2.advanceVersion))
      val expectedMergeResponse = Right(savedArticles2.advanceVersion)

      savedArticlesPersistence.read(userId) returns(Success(Some(savedArticles)))
      savedArticlesPersistence.update(argThat(===(userId)), argThat(===(savedArticles2))) returns(responseArticles)
      val saved = savedArticlesMerger.updateWithRetryAndMerge(userId, savedArticles2)
      there was one(savedArticlesPersistence).read(argThat(===(userId)))
      there were no(savedArticlesPersistence).write(any[String](), any[SavedArticles]())
      there was one(savedArticlesPersistence).update(argThat(===(userId)), argThat(===(savedArticles2)))
      saved shouldEqual (expectedMergeResponse)
    }

    "will merge articles if there is a cnnflict" in new Setup {
      val articlesCurrentlySaved = SavedArticles("2", List(article1, article2))
      val articlesToSave = SavedArticles("1", List(article1, article2, article3))
      val expectedMergedArticles = SavedArticles("2", List(article1, article2, article3))
      savedArticlesPersistence.read(userId) returns(Success(Some(articlesCurrentlySaved)))
      savedArticlesPersistence.update(any[String](), any[SavedArticles]()) returns(Success(Some(expectedMergedArticles.advanceVersion)))
      savedArticlesMerger.updateWithRetryAndMerge(userId, articlesToSave)
      there was one(savedArticlesPersistence).update(argThat(===(userId)), argThat(===(expectedMergedArticles)))
    }

    "will dedupe merged articles if there is a conflict" in new Setup {
      val articlesCurrentlySaved = SavedArticles("2", List(article1Dup, article2))
      val articlesToSave = SavedArticles("1", List(article1, article2, article3))
      val expectedMergedArticles = SavedArticles("2", List(article1, article2, article3))
      savedArticlesPersistence.read(userId) returns(Success(Some(articlesCurrentlySaved)))
      savedArticlesPersistence.update(any[String](), any[SavedArticles]()) returns(Success(Some(expectedMergedArticles.advanceVersion)))
      savedArticlesMerger.updateWithRetryAndMerge(userId, articlesToSave)
      there was one(savedArticlesPersistence).update(argThat(===(userId)), argThat(===(expectedMergedArticles)))
    }
    

    "will not try to merge a list of articles with a length greater than the saved article limit" in new Setup {
      private val maxSavedArticlesLimit = 2
      override val savedArticlesMerger = new SavedArticlesMergerImpl(SavedArticlesMergerConfig(maxSavedArticlesLimit), savedArticlesPersistence)
      val saved = savedArticlesMerger.updateWithRetryAndMerge(userId, savedArticles2)
      there were no (savedArticlesPersistence).read(argThat(===(userId)))
      there were no (savedArticlesPersistence).write(any[String](), any[SavedArticles]())
      there were no (savedArticlesPersistence).update(any[String](), any[SavedArticles]())
      saved mustEqual(Left(MaxSavedArticleTransgressionError(s"The limit on number of saved articles is $maxSavedArticlesLimit")))
    }

    "will dedupe articles before checking whether the article limit hs been transgressed" in new Setup {
      private val maxSavedArticlesLimit = 3
      override val savedArticlesMerger = new SavedArticlesMergerImpl(SavedArticlesMergerConfig(maxSavedArticlesLimit), savedArticlesPersistence)
      val savedArticlesWithDupes = savedArticles2.copy(articles = List(article1, article1Dup, article2, article2Dup))
      val responseArticles = Success(Some(savedArticles.advanceVersion))
      val expectedMergeResponse = Right(savedArticles.advanceVersion)
      val expectedDeduped = SavedArticles(version, List(article1, article2))

      savedArticlesPersistence.read(userId) returns (Success(None))
      savedArticlesPersistence.write(any[String](), any[SavedArticles]()) returns (responseArticles)
      savedArticlesMerger.updateWithRetryAndMerge(userId, savedArticlesWithDupes)
      there was one(savedArticlesPersistence).read(argThat(===(userId)))
      there was one(savedArticlesPersistence).write(argThat(===(userId)), argThat(===(expectedDeduped)))

    }

    "failure to get current articles throws the correct exception" in new Setup {
      savedArticlesPersistence.read(userId) returns (Failure(new IllegalStateException("Bad, bad, bad")))
      val saved = savedArticlesMerger.updateWithRetryAndMerge(userId, savedArticles)
      there was one(savedArticlesPersistence).read(argThat(===(userId)))
      saved shouldEqual (Left(SavedArticleMergeError("Could not retrieve current articles")))
    }

    "failure to update the saved articles results in the currect error" in new Setup {
      savedArticlesPersistence.read(userId) returns (Success(None))
      savedArticlesPersistence.write(userId, savedArticles) returns (Failure(new IllegalStateException("My mummy told me to be good, but I was naughty")))

      val saved = savedArticlesMerger.updateWithRetryAndMerge(userId, savedArticles)
      saved shouldEqual (Left(SavedArticleMergeError("Could not update articles")))
    }
  }

  trait Setup extends Scope {
    val savedArticlesPersistence = mock[SavedArticlesPersistenceImpl]
    val savedArticlesMerger = new SavedArticlesMergerImpl(SavedArticlesMergerConfig(20), savedArticlesPersistence)
  }

}
