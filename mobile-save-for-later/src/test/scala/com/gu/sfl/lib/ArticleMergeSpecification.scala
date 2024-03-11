package com.gu.sfl.lib

import java.time.LocalDateTime

import com.gu.sfl.exception.SavedArticleMergeError
import com.gu.sfl.model.{SavedArticle, SavedArticles}
import com.gu.sfl.persistence.SavedArticlesPersistenceImpl
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.util.{Failure, Success}

class ArticleMergeSpecification extends Specification with Mockito {

  "updateSavedArticlesWithRetryAndMerge" should {
    val userId = "123"
    val version = "1"

    val (article1, article2, article3, article4) = (
      SavedArticle(
        "id/1",
        "p/1",
        LocalDateTime.of(2018, 1, 16, 16, 30),
        read = true
      ),
      SavedArticle(
        "id/2",
        "p/2",
        LocalDateTime.of(2018, 2, 17, 17, 30),
        read = false
      ),
      SavedArticle(
        "id/3",
        "p/3",
        LocalDateTime.of(2018, 3, 18, 18, 30),
        read = true
      ),
      SavedArticle(
        "id/4",
        "p/4",
        LocalDateTime.of(2018, 4, 19, 19, 30),
        read = true
      )
    )

    val savedArticles = SavedArticles(version, List(article1, article2))
    val savedArticlesUpdate1 = SavedArticles(version, List(article3))
    val savedArticles2 =
      SavedArticles(version, List(article1, article2, article3))
    val article1Dup = SavedArticle(
      "id/1",
      "p/1",
      LocalDateTime.of(2017, 1, 16, 16, 30),
      read = true
    )
    val article2Dup = SavedArticle(
      "id/2",
      "p/2",
      LocalDateTime.of(2017, 3, 17, 17, 30),
      read = false
    )

    "saves the articles if the user does not currently have any articles saved" in new Setup {
      val responseArticles = Success(Some(savedArticles.advanceVersion))
      val expectedMergeResponse = Right(savedArticles.advanceVersion)

      savedArticlesPersistence.read(userId) returns (Success(None))
      savedArticlesPersistence.update(
        userId,
        savedArticles
      ) returns (responseArticles)
      val saved =
        savedArticlesMerger.updateWithRetryAndMerge(userId, savedArticles)
      there was one(savedArticlesPersistence).read(userId)
      there was one(savedArticlesPersistence).update(userId, savedArticles)
      saved shouldEqual (expectedMergeResponse)
    }

    "will update the the users' saved articles if there is no conflict" in new Setup {
      val responseArticles = Success(Some(savedArticles2.advanceVersion))
      val expectedMergeResponse = Right(savedArticles2.advanceVersion)

      savedArticlesPersistence.read(userId) returns (Success(
        Some(savedArticles)
      ))
      savedArticlesPersistence.update(
        argThat(===(userId)),
        argThat(===(savedArticles2))
      ) returns (responseArticles)
      val saved =
        savedArticlesMerger.updateWithRetryAndMerge(userId, savedArticles2)
      there was one(savedArticlesPersistence).read(argThat(===(userId)))
      there was one(savedArticlesPersistence).update(
        argThat(===(userId)),
        argThat(===(savedArticles2))
      )
      saved shouldEqual (expectedMergeResponse)
    }

    "will not persist saved articles if they are identical to the articles currently saved and there is no conflict" in new Setup {
      savedArticlesPersistence.read(userId) returns (Success(
        Some(savedArticles)
      ))

      val saved =
        savedArticlesMerger.updateWithRetryAndMerge(userId, savedArticles)
      there was one(savedArticlesPersistence).read(argThat(===(userId)))
      there were no(savedArticlesPersistence).update(
        any[String](),
        any[SavedArticles]()
      )

      saved shouldEqual (Right(savedArticles))

    }

    "will merge articles if there is a cnnflict" in new Setup {
      val articlesCurrentlySaved = SavedArticles("2", List(article1, article2))
      val articlesToSave =
        SavedArticles("1", List(article1, article2, article3))
      val expectedMergedArticles =
        SavedArticles("2", List(article1, article2, article3))
      savedArticlesPersistence.read(userId) returns (Success(
        Some(articlesCurrentlySaved)
      ))
      savedArticlesPersistence.update(
        any[String](),
        any[SavedArticles]()
      ) returns (Success(Some(expectedMergedArticles.advanceVersion)))
      savedArticlesMerger.updateWithRetryAndMerge(userId, articlesToSave)
      there was one(savedArticlesPersistence).update(
        argThat(===(userId)),
        argThat(===(expectedMergedArticles))
      )
    }

    "will dedupe merged articles if there is a conflict" in new Setup {
      val articlesCurrentlySaved =
        SavedArticles("2", List(article1Dup, article2))
      val articlesToSave =
        SavedArticles("1", List(article1, article2, article3))
      val expectedMergedArticles =
        SavedArticles("2", List(article1, article2, article3))
      savedArticlesPersistence.read(userId) returns (Success(
        Some(articlesCurrentlySaved)
      ))
      savedArticlesPersistence.update(
        any[String](),
        any[SavedArticles]()
      ) returns (Success(Some(expectedMergedArticles.advanceVersion)))
      savedArticlesMerger.updateWithRetryAndMerge(userId, articlesToSave)
      there was one(savedArticlesPersistence).update(
        argThat(===(userId)),
        argThat(===(expectedMergedArticles))
      )
    }

    "will select the latest articles up to the limit where the article limit is broken and there is no conflict`" in new Setup {
      private val maxSavedArticlesLimit = 2

      override val savedArticlesMerger = new SavedArticlesMergerImpl(
        SavedArticlesMergerConfig(maxSavedArticlesLimit),
        savedArticlesPersistence
      )
      val expectedArticlesPersisted =
        savedArticles.copy(articles = List(article2, article3))
      val expectedMergeResponse =
        Right(expectedArticlesPersisted.advanceVersion)
      savedArticlesPersistence.update(
        any[String](),
        any[SavedArticles]()
      ) returns (Success(Some(expectedArticlesPersisted.advanceVersion)))
      savedArticlesPersistence.read(userId) returns (Success(
        Some(savedArticles)
      ))
      val saved =
        savedArticlesMerger.updateWithRetryAndMerge(userId, savedArticles2)

      there was one(savedArticlesPersistence).update(
        argThat(===(userId)),
        argThat(===(expectedArticlesPersisted))
      )
      saved mustEqual (expectedMergeResponse)
    }

    "will select the latest articles up to the limit where the article limit is broken and there is a conflict" in new Setup {
      private val maxSavedArticlesLimit = 2
      override val savedArticlesMerger = new SavedArticlesMergerImpl(
        SavedArticlesMergerConfig(maxSavedArticlesLimit),
        savedArticlesPersistence
      )

      val articlesCurrentlySaved = SavedArticles("2", List(article1, article2))
      val articlesToSave =
        SavedArticles("1", List(article1, article2, article3))

      //Returns an object consisting of the version currently saved and the latest 2 of th
      val expectedArticlesPersisted =
        SavedArticles("2", List(article2, article3))
      val expectedMergeResponse =
        Right(expectedArticlesPersisted.advanceVersion)

      savedArticlesPersistence.read(any[String]()) returns (Success(
        Some(articlesCurrentlySaved)
      ))
      savedArticlesPersistence.update(
        any[String](),
        any[SavedArticles]()
      ) returns (Success(Some(expectedArticlesPersisted.advanceVersion)))
      val saved =
        savedArticlesMerger.updateWithRetryAndMerge(userId, articlesToSave)
      there was one(savedArticlesPersistence).update(
        argThat(===(userId)),
        argThat(===(expectedArticlesPersisted))
      )
      saved mustEqual (expectedMergeResponse)
    }

    "will dedupe articles before checking whether the article limit hs been transgressed" in new Setup {
      private val maxSavedArticlesLimit = 3
      override val savedArticlesMerger = new SavedArticlesMergerImpl(
        SavedArticlesMergerConfig(maxSavedArticlesLimit),
        savedArticlesPersistence
      )
      val savedArticlesWithDupes = savedArticles2.copy(articles =
        List(article1, article1Dup, article2, article2Dup)
      )
      val responseArticles = Success(Some(savedArticles.advanceVersion))
      val expectedMergeResponse = Right(savedArticles.advanceVersion)
      val expectedDeduped = SavedArticles(version, List(article1, article2))

      savedArticlesPersistence.read(userId) returns (Success(None))
      savedArticlesPersistence.update(
        any[String](),
        any[SavedArticles]()
      ) returns (responseArticles)
      savedArticlesMerger.updateWithRetryAndMerge(
        userId,
        savedArticlesWithDupes
      )
      there was one(savedArticlesPersistence).read(argThat(===(userId)))
      there was one(savedArticlesPersistence).update(
        argThat(===(userId)),
        argThat(===(expectedDeduped))
      )

    }

    "failure to get current articles throws the correct exception" in new Setup {
      savedArticlesPersistence.read(userId) returns (Failure(
        new IllegalStateException("Bad, bad, bad")
      ))
      val saved =
        savedArticlesMerger.updateWithRetryAndMerge(userId, savedArticles)
      there was one(savedArticlesPersistence).read(argThat(===(userId)))
      saved shouldEqual (Left(
        SavedArticleMergeError("Could not retrieve current articles")
      ))
    }

    "failure to update the saved articles results in the currect error" in new Setup {
      savedArticlesPersistence.read(userId) returns (Success(None))
      savedArticlesPersistence.update(userId, savedArticles) returns (Failure(
        new IllegalStateException(
          "My mummy told me to be good, but I was naughty"
        )
      ))

      val saved =
        savedArticlesMerger.updateWithRetryAndMerge(userId, savedArticles)
      saved shouldEqual (Left(
        SavedArticleMergeError("Could not update articles")
      ))
    }
  }

  trait Setup extends Scope {
    val savedArticlesPersistence = mock[SavedArticlesPersistenceImpl]
    val savedArticlesMerger = new SavedArticlesMergerImpl(
      SavedArticlesMergerConfig(20),
      savedArticlesPersistence
    )
  }

}
