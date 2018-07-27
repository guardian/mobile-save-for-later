package coml.gu.sfl.userdeletion.db

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.dynamodbv2._
import com.gu.scanamo.Scanamo.exec
import com.gu.scanamo.Table
import com.gu.scanamo.syntax._
import com.gu.sfl.Logging
import com.gu.sfl.model.DynamoSavedArticles
import com.gu.sfl.persistance.PersistanceConfig
import com.gu.sfl.userdeletion.model.UserDeleteMessage

class SflDynamoDb(persistanceConfig: PersistanceConfig) extends Logging {

  private val table = Table[DynamoSavedArticles](persistanceConfig.tableName)
  private val client: AmazonDynamoDB = AmazonDynamoDBClient.builder().withCredentials(DefaultAWSCredentialsProviderChain.getInstance()).build()

  def deleteSavedArticleasForUser(user: UserDeleteMessage) = {
    logger.info(s"Deleting record for user id: ${user.userId}")
    Option(exec(client)(table.delete('userId -> user.userId))).isDefined match {
      case true => logger.info("Deleted ok")
      case false => logger.info("unable to delete user record")
    }
  }

}
