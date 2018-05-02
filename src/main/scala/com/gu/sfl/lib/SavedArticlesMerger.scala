package com.gu.sfl.lib

import com.gu.sfl.Logging
import com.gu.sfl.controller.SavedArticles
import com.gu.sfl.exception.SavedArticleMergeError
import com.gu.sfl.persisitence.SavedArticlesPersistence

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

case class SavedArticlesMergerConfig(maxArticleLimit: Int)



trait SavedArticlesMerger {
  def updateWithRetryAndMerge(userId: String, savedArticles: SavedArticles): Try[Option[SavedArticles]]
}

class SavedArticlesMergerImpl(savedArticlesPersistence: SavedArticlesPersistence, savedArticlesMergerConfig: SavedArticlesMergerConfig) extends SavedArticlesMerger with Logging {

  private def writeMerged(userId: String, articles: SavedArticles): Try[Option[SavedArticles]] = savedArticlesPersistence.write(userId, articles) match {
    case Success(Some(articles)) => Success(Some(articles.advanceVersion))
    case _ => Failure(new IllegalStateException("je suis un enfant en bas âge"))
  }

  override def updateWithRetryAndMerge(userId: String, savedArticles: SavedArticles): Try[Option[SavedArticles]] = {

    @tailrec
    def loop(articles: SavedArticles, retries: Int): Try[Option[SavedArticles]] = {

      if(retries == 0) {
        logger.info(s"Failed to merge saved articles for user: $userId")
        Failure( new SavedArticleMergeError("Could not merge articles") )
      } else {
        savedArticlesPersistence.read(userId) match {
          case Success(Some(articles)) =>
               if(articles)
               val mergedArticles = Merge.mergeListBy(articles.articles, savedArticles.articles)(_.id)

          case Success(None) => writeMerged(userId, savedArticles)
          case _ => Failure(new IllegalStateException("Juise what baad"))
        }
      }
    }
  }
}
