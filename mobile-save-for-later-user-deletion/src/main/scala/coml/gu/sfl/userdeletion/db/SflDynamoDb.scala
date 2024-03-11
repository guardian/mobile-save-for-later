package coml.gu.sfl.userdeletion.db

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import org.scanamo.{Scanamo, Table}
import org.scanamo.syntax._
import com.gu.sfl.Logging
import com.gu.sfl.persistence.{DynamoSavedArticles, PersistenceConfig}
import com.gu.sfl.userdeletion.model.UserDeleteMessage
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider

class SflDynamoDb(persistanceConfig: PersistenceConfig) extends Logging {

  private val table = Table[DynamoSavedArticles](persistanceConfig.tableName)
  private val client =
    DynamoDbClient
      .builder()
      .credentialsProvider(DefaultCredentialsProvider)
      .build()
  private val scanamo = Scanamo(client)

  def deleteSavedArticleasForUser(user: UserDeleteMessage) = {
    logger.info(s"Deleting record for user id: ${user.userId}")
    logger.info(
      Option(scanamo.exec(table.delete("userId" -> user.userId)))
        .fold(s"Unable to delete record for user ${user.userId}")(_ =>
          s"Deleted record for ${user.userId}"
        )
    )
  }

}
