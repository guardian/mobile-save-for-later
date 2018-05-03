package com.gu.sfl.lib

import com.gu.sfl.Logging
import com.gu.sfl.controller.SavedArticles
import com.gu.sfl.exception.{MaxSavedArticleTransgressionError, SavedArticleMergeError}
import com.gu.sfl.persisitence.SavedArticlesPersistence

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

case class SavedArticlesMergerConfig(maxSavedArticlesLimit: Int)

trait SavedArticlesMerger {
  def updateWithRetryAndMerge(userId: String, savedArticles: SavedArticles): Try[Option[SavedArticles]]
}

class SavedArticlesMergerImpl(savedArticlesMergerConfig: SavedArticlesMergerConfig, savedArticlesPersistence: SavedArticlesPersistence) extends SavedArticlesMerger with Logging {

  val maxfSavedArticlesLimit = savedArticlesMergerConfig.maxSavedArticlesLimit

  private def writeMerged(userId: String, articles: SavedArticles): Try[Option[SavedArticles]] = savedArticlesPersistence.update(userId, articles) match {
    case Success(Some(articles)) =>
      logger.info(s"Got back following articles: $articles")
      Success(Some(articles))
    case _ => Failure(new IllegalStateException("je suis un enfant en bas âge"))
  }

  override def updateWithRetryAndMerge(userId: String, savedArticles: SavedArticles): Try[Option[SavedArticles]] = {

    @tailrec
    def loop(articles: SavedArticles, retries: Int): Try[Option[SavedArticles]] = {

      if(retries == 0) {
        logger.info(s"Failed to merge saved articles for user: $userId")
        Failure( new SavedArticleMergeError("Conflicting version in savedArticles") )
      } else {
        savedArticlesPersistence.read(userId) match {
          case Success(Some(currentArticles)) =>
               val mergedArticles = Merge.mergeListBy(articles.articles, savedArticles.articles)(_.id)
               if(currentArticles.version == articles.version) {
                 logger.debug(s"Merging new articles list for user: $userId")
                 writeMerged(userId, SavedArticles(articles.version, mergedArticles))
               }
               else {
                 logger.info(s"Conflicting merge try on saving articles. trying again")
                 val nextTryArticles = SavedArticles(currentArticles.version, mergedArticles)
                 loop(articles, retries - 1)
               }

          case Success(None) =>
            logger.info("Adding articles for new user")
            savedArticlesPersistence.write(userId, articles)
          case _ => Failure(new IllegalStateException("Juise what baad"))
        }
      }
    }

    if( savedArticles.articles.length > maxfSavedArticlesLimit) {
      logger.info(s"User $userId tried to save ${savedArticles.articles.length} articles. Limit is ${maxfSavedArticlesLimit}.")
      Failure(new MaxSavedArticleTransgressionError(s"Tried to save more than $maxfSavedArticlesLimit articles.") )
    } else {
      loop(savedArticles, 2)
    }
  }
}
