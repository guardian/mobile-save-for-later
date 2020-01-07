package com.gu.sfl.lib

import com.amazonaws.util.Md5Utils
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
          persistMergedArticles(userId, deduplicatedArticles)(savedArticlesPersistence.update)
        }
        else
          Right(deduplicatedArticles)
      case Success(Some(currentArticles)) =>
        logger.info(s"Received version ${deduplicatedArticles.version} from the client but had version ${currentArticles.version} in the database")
        val articlesToSave = currentArticles.copy(articles = MergeLogic.mergeListBy(currentArticles.articles, deduplicatedArticles.articles)(_.id))
        persistMergedArticles(userId, articlesToSave)(savedArticlesPersistence.update)
      case Success(None) =>
        persistMergedArticles(userId, deduplicatedArticles)(savedArticlesPersistence.write)
      case _ => Left(SavedArticleMergeError("Could not retrieve current articles"))
    }
  }
}
