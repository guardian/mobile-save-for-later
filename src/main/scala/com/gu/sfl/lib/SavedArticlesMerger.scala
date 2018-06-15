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

    @tailrec
    def loop(articles: SavedArticles, retries: Int): Try[Option[SyncedPrefs]] = {

      if(retries == 0) {
        logger.info(s"Failed to merge saved articles for user: $userId")
        Failure(SavedArticleMergeError("Conflicting version in savedArticles") )
      } else {
        savedArticlesPersistence.read(userId) match {
          case Success(Some(currentArticles)) =>
            val mergedArticles = Merge.mergeListBy(currentArticles.articles, articles.articles)(_.id)
            if(currentArticles.version == articles.version) {
               logger.debug(s"Merging new articles list for user: $userId")
               persistMergedArticles(userId, SavedArticles(articles.version, mergedArticles))(persistOperation = savedArticlesPersistence.update)
             }
             else {
               logger.info(s"Conflicting merge try on saving articles. trying again")
               val nextTryArticles = currentArticles.copy(articles = mergedArticles)
               loop(nextTryArticles, retries - 1)
             }

          case Success(None) =>
            logger.info("Adding articles for new user")
             persistMergedArticles(userId, articles)(savedArticlesPersistence.write)
          case _ => Failure(SavedArticleMergeError("Could not retrieve current articles"))
        }
      }
    }

    if( savedArticles.articles.lengthCompare(maxSavedArticlesLimit) > 0 ){
      logger.info(s"User $userId tried to save ${savedArticles.articles.length} articles. Limit is ${maxSavedArticlesLimit}.")
      Failure(MaxSavedArticleTransgressionError(s"Tried to save more than $maxSavedArticlesLimit articles.") )
    } else {
      loop(savedArticles, 3)
    }
  }
}
