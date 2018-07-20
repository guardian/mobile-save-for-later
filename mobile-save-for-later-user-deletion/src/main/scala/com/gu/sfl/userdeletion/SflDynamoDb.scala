package com.gu.sfl.userdeletion

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.dynamodbv2._
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult
import com.gu.sfl.logging.Logging
import com.gu.scanamo.Table
import com.gu.scanamo.Scanamo.exec
import com.gu.scanamo.syntax._
import com.gu.sfl.userdeletion.model.User


case class DynamoSavedArticles(userId: String, version: String, articles: String)

case class PersistanceConfig(appName: String, stage: String) {
  val tableName = s"$appName-$stage-articles"
}

class SflDynamoDb(persistanceConfig: PersistanceConfig) extends Logging {

  private val table = Table[DynamoSavedArticles](persistanceConfig.tableName)
  private val client: AmazonDynamoDB = AmazonDynamoDBClient.builder().withCredentials(DefaultAWSCredentialsProviderChain.getInstance()).build()

  def deleteSavedArticleasForUser(user: User) : Boolean = {
    logger.info(s"Deleting record for user id: ${user.userId}")
    val r = Option(exec(client)(table.delete('userId -> user.userId))).isDefined
    r match {
      case true => logger.info("Deleted ok")
      case false => logger.info("unable to delete user record")
    }
    r
  }

}
