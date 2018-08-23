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

    val deduplicatedArticles = getDedupedArticles(savedArticles)

    savedArticlesPersistence.read(userId) match {
      case Success(Some(currentArticles)) if currentArticles.version == deduplicatedArticles.version =>
        if(currentArticles != deduplicatedArticles) {
          logSomeOfReadAndToWrite(currentArticles, savedArticles)
          persistMergedArticles(userId, deduplicatedArticles)(savedArticlesPersistence.update)
        }
        else
          Right(deduplicatedArticles)
      case Success(Some(currentArticles)) =>
        logSomeOfReadAndToWrite(currentArticles, savedArticles)
        val articlesToSave = currentArticles.copy(articles = MergeLogic.mergeListBy(currentArticles.articles, deduplicatedArticles.articles)(_.id))
        persistMergedArticles(userId, articlesToSave)(savedArticlesPersistence.update)
      case Success(None) =>
        persistMergedArticles(userId, deduplicatedArticles)(savedArticlesPersistence.write)
      case _ => Left(SavedArticleMergeError("Could not retrieve current articles"))
    }
  }
  private val bigInt10000 = BigInt(10000)
  private val bigInt9999 = BigInt(9999)
  private def logSomeOfReadAndToWrite(existingArticles: SavedArticles, requestedArticles: SavedArticles): Unit = {
    val readArticlesJson = Jackson.mapper.writeValueAsString(existingArticles)
    if ((BigInt(Md5Utils.computeMD5Hash(readArticlesJson.getBytes)) % bigInt10000) == bigInt9999) {
      logger.info(s"WRITE_COMPARISON: Read: $readArticlesJson and Write: ${Jackson.mapper.writeValueAsString(requestedArticles)}")
    }
  }

  //This is done here for debugging puposes. To be removed when we are connfident it's no longer needed
  private def getDedupedArticles(savedArticles: SavedArticles) : SavedArticles = {
    val deDupedArticles = savedArticles.deduped
    logger.info(s"Saving articles - Number of raw articles from client: ${savedArticles.numberOfArticles}, Number with dupicates removed: ${deDupedArticles.numberOfArticles} ")
    deDupedArticles
  }
}
