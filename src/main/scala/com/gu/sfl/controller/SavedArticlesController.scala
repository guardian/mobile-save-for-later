package com.gu.sfl.controller

import com.gu.sfl.Logging
import com.gu.sfl.lambda.{LambdaRequest, LambdaResponse}
import com.gu.sfl.persisitence.SavedArticlesPersistence
import com.gu.sfl.services.IdentityService
import com.gu.sfl.util.StatusCodes
import com.gu.sfl.lib.Jackson.mapper

import scala.util.Success

object SavedArticlesController {
  val missingUserResponse = LambdaResponse(StatusCodes.badRequest, Some(Left("Could not find a user ")))
  val emptyArticlesResponse = LambdaResponse(StatusCodes.ok, Some(Left(mapper.writeValueAsString(SavedArticles(List.empty)))))

}

class SavedArticlesController(identityService: IdentityService, savedArticlesPersistence: SavedArticlesPersistence) extends Function[LambdaRequest, LambdaResponse] with Logging {
  override def apply(lambdaRequest: LambdaRequest): LambdaResponse = {
    (for{
       userId <- identityService.userFromRequest(lambdaRequest)
    } yield {
      savedArticlesPersistence.read(userId) match {
        case Success(Some(savedArticles)) =>
          logger.info(s"Retrieved ${savedArticles.articles.size} saved articles for user id: ${userId}")
          LambdaResponse(StatusCodes.ok, Some(Left(mapper.writeValueAsString(savedArticles))))
        case _ =>
          logger.info(s"No articles found for user: $userId")
          SavedArticlesController.emptyArticlesResponse
      }
    }).getOrElse(SavedArticlesController.missingUserResponse)
  }
}
