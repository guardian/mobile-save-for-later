package coml.gu.sfl.userdeletion.db

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.dynamodbv2._
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult
import org.scanamo.{Scanamo, Table}
import org.scanamo.syntax._
import com.gu.sfl.Logging
import com.gu.sfl.persistence.{DynamoSavedArticles, PersistenceConfig}
import com.gu.sfl.userdeletion.model.UserDeleteMessage
import org.scanamo.DeleteReturn.OldValue
import org.scanamo.generic.auto.genericDerivedFormat
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

class SflDynamoDb(persistanceConfig: PersistenceConfig) extends Logging {

  private val table = Table[DynamoSavedArticles](persistanceConfig.tableName)
  private val client = DynamoDbClient.create()
  private val scanamo = Scanamo(client)
  def deleteSavedArticleasForUser(user: UserDeleteMessage) = {
    logger.info(s"Deleting record for user id: ${user.userId}")
    val dbResponse = scanamo.exec(table.deleteAndReturn(OldValue)("userId" === user.userId))
      .fold(s"Unable to delete record for user ${user.userId}")((_) => s"Deleted record for ${user.userId}")
    logger.info(dbResponse)
  }

}
