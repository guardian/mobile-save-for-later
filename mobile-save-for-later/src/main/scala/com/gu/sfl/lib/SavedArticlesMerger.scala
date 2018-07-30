package com.gu.sfl.lib

import com.gu.sfl.Logging
import com.gu.sfl.exception.{SaveForLaterError, SavedArticleMergeError}
import com.gu.sfl.model._
import com.gu.sfl.persistence.SavedArticlesPersistence

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

case class SavedArticlesMergerConfig(maxSavedArticlesLimit: Int)

trait SavedArticlesMerger {
  def updateWithRetryAndMerge(userId: String, savedArticles: SavedArticles): Either[SaveForLaterError, SavedArticles]
}

class SavedArticlesMergerImpl(savedArticlesMergerConfig: SavedArticlesMergerConfig, savedArticlesPersistence: SavedArticlesPersistence) extends SavedArticlesMerger with Logging {

  val maxSavedArticlesLimit: Int = savedArticlesMergerConfig.maxSavedArticlesLimit

  private def persistMergedArticles(userId: String, articles: SavedArticles)( persistOperation: (String, SavedArticles) => Try[Option[SavedArticles]] ): Either[SaveForLaterError, SavedArticles] = {
    val articlesToPersist = articles.mostRecent(savedArticlesMergerConfig.maxSavedArticlesLimit)
    persistOperation(userId, articlesToPersist) match {
      case Success(Some(articles)) =>
        logger.debug(s"success persisting articles for ${userId}")
        Right(articles)
      case Failure(e) =>
        logger.debug(s"Error persisting articles for ${userId}. Error: ${e.getMessage}")
        Left(SavedArticleMergeError("Could not update articles"))
    }
  }


  override def updateWithRetryAndMerge(userId: String, savedArticles: SavedArticles): Either[SaveForLaterError, SavedArticles] = {

    val deduplicatedArticles = getDedupedArticles(savedArticles)

    savedArticlesPersistence.read(userId) match {
      case Success(Some(currentArticles)) if currentArticles.version == deduplicatedArticles.version =>
        persistMergedArticles(userId, deduplicatedArticles)(savedArticlesPersistence.update)
      case Success(Some(currentArticles)) =>
        val articlesToSave = currentArticles.copy(articles = MergeLogic.mergeListBy(currentArticles.articles, deduplicatedArticles.articles)(_.id))
        persistMergedArticles(userId, articlesToSave)(savedArticlesPersistence.update)
      case Success(None) =>
        persistMergedArticles(userId, deduplicatedArticles)(savedArticlesPersistence.write)
      case _ => Left(SavedArticleMergeError("Could not retrieve current articles"))
    }
  }

  //This is done here for debugging puposes. To be removed when we are connfident it's no longer needed
  private def getDedupedArticles(savedArticles: SavedArticles) : SavedArticles = {
    val deDupedArticles = savedArticles.deduped
    logger.info(s"Saving articles - Number of raw articles from client: ${savedArticles.numberOfArticles}, Number with dupicates removed: ${deDupedArticles.numberOfArticles} ")
    deDupedArticles
  }
}
