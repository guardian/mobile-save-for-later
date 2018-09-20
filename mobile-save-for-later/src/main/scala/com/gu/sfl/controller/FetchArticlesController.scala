package com.gu.sfl.controller

import com.gu.sfl.exception.{IdentityServiceError, MissingAccessTokenError, UserNotFoundError}
import com.gu.sfl.lambda.{LambdaRequest, LambdaResponse}
import com.gu.sfl.lib.CloudWatchPublisher
import com.gu.sfl.savedarticles.FetchSavedArticles

import scala.concurrent.{ExecutionContext, Future}

class FetchArticlesController(fetchSavedArticles: FetchSavedArticles, cloudWatchPublisher: CloudWatchPublisher)(implicit executionContext: ExecutionContext) extends Function[LambdaRequest, Future[LambdaResponse]] with SaveForLaterController {

  override def defaultErrorMessage: String = "Error fetching articles"

  override def apply(lambdaRequest: LambdaRequest): Future[LambdaResponse] = {

    val futureResponse = fetchSavedArticles.retrieveForUser(lambdaRequest.headers).map {
      case Right(syncedPrefs) =>
        syncedPrefs.savedArticles.foreach(sp =>
          logger.debug(s"Returning found: ${sp.articles.size} articles")
        )
        okSyncedPrefsResponse(syncedPrefs)
      case Left(error) =>
        logger.error(s"Error trying to retrieve saved articles: ${error.message}")
        processErrorResponse(error) {
          case e: IdentityServiceError => identityErrorResponse
          case e: MissingAccessTokenError => missingAccessTokenResponse
          case e: UserNotFoundError => missingUserResponse
        }
    }
    futureResponse.transform(attempt => {
      cloudWatchPublisher.sendMetricsSoFar()
      attempt
    })
  }
}
