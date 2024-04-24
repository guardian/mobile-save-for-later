package com.gu.sfl.lib

import com.gu.sfl.Logging
import com.gu.sfl.exception.{SaveForLaterError, SavedArticleMergeError}
import com.gu.sfl.model._
import com.gu.sfl.persistence.SavedArticlesPersistence

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

    val deduplicatedArticles = savedArticles.deduped

    savedArticlesPersistence.read(userId) match {
      case Success(Some(currentArticles)) if currentArticles.version == deduplicatedArticles.version =>
        if(currentArticles != deduplicatedArticles) {
          logger.info(s"UserId: $userId. Received version ${deduplicatedArticles.version} which matched the database. DB count: ${currentArticles.articles.length}, client count: ${deduplicatedArticles.articles.length}")
          persistMergedArticles(userId, deduplicatedArticles)(savedArticlesPersistence.update)
        }
        else
          Right(deduplicatedArticles)
      case Success(Some(currentArticles)) =>
        val articlesToSave = currentArticles.copy(articles = MergeLogic.mergeListBy(currentArticles.articles, deduplicatedArticles.articles)(_.id))
        logger.info(s"UserId: $userId. Received version ${deduplicatedArticles.version} from the client but had version ${currentArticles.version} in the database. DB count: ${currentArticles.articles.length}, client count: ${deduplicatedArticles.articles.length}, merged count: ${articlesToSave.articles.length}")
        persistMergedArticles(userId, articlesToSave)(savedArticlesPersistence.update)
      case Success(None) =>
        logger.info(s"UserId: $userId. Storing articles for the first time. Version: ${deduplicatedArticles.version}. Client count: ${deduplicatedArticles.articles.length}")
        persistMergedArticles(userId, deduplicatedArticles)(savedArticlesPersistence.update)
      case _ => Left(SavedArticleMergeError("Could not retrieve current articles"))
    }
  }
}
