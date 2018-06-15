package com.gu.sfl.lib

import com.gu.sfl.Logging
import com.gu.sfl.exception.{MaxSavedArticleTransgressionError, SavedArticleMergeError}
import com.gu.sfl.model._
import com.gu.sfl.persisitence.SavedArticlesPersistence

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

case class SavedArticlesMergerConfig(maxSavedArticlesLimit: Int)

trait SavedArticlesMerger {
  def updateWithRetryAndMerge(userId: String, savedArticles: SavedArticles): Try[Option[SyncedPrefs]]
}

class SavedArticlesMergerImpl(savedArticlesMergerConfig: SavedArticlesMergerConfig, savedArticlesPersistence: SavedArticlesPersistence) extends SavedArticlesMerger with Logging {

  val maxSavedArticlesLimit: Int = savedArticlesMergerConfig.maxSavedArticlesLimit

  private def persistMergedArticles(userId: String, articles: SavedArticles)( persistOperation: (String, SavedArticles) => Try[Option[SavedArticles]] ): Try[Option[SyncedPrefs]] = persistOperation(userId, articles) match {
    case Success(Some(articles)) =>
      logger.info(s"success persisting articles for ${userId}")
      Success(Some(SyncedPrefs(userId, Some(articles))))

    case Failure(e) =>
      logger.info(s"Error persisting articles for ${userId}. Error: ${e.getMessage}")
      Failure(SavedArticleMergeError("Could not update articles"))
  }

  override def updateWithRetryAndMerge(userId: String, savedArticles: SavedArticles): Try[Option[SyncedPrefs]] = {

    if( savedArticles.articles.lengthCompare(maxSavedArticlesLimit) > 0 ){
      logger.info(s"User $userId tried to save ${savedArticles.articles.length} articles. Limit is ${maxSavedArticlesLimit}.")
      Failure(MaxSavedArticleTransgressionError(s"Tried to save more than $maxSavedArticlesLimit articles.") )
    } else {
      savedArticlesPersistence.read(userId) match {
        case Success(Some(currentArticles)) if currentArticles.version == savedArticles.version =>
          persistMergedArticles(userId, savedArticles)(savedArticlesPersistence.update)
        case Success(Some(currentArticles)) =>
          val articlesToSave = savedArticles.copy(articles = Merge.mergeListBy(currentArticles.articles, savedArticles.articles)(_.id))
          persistMergedArticles(userId, articlesToSave)(savedArticlesPersistence.update)
        case Success(None) =>
          logger.info("Adding articles for new user")
          persistMergedArticles(userId, savedArticles)(savedArticlesPersistence.write)
        case _ => Failure(SavedArticleMergeError("Could not retrieve current articles"))
      }

    }
  }
}
