package coml.gu.sfl.userdeletion.db

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.dynamodbv2._
import com.gu.scanamo.Scanamo.exec
import com.gu.scanamo.Table
import com.gu.scanamo.syntax._
import com.gu.sfl.Logging
import com.gu.sfl.model.DynamoSavedArticles
import com.gu.sfl.persistance.PersistenceConfig
import com.gu.sfl.userdeletion.model.User

class SflDynamoDb(persistanceConfig: PersistenceConfig) extends Logging {

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
