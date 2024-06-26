package coml.gu.sfl.userdeletion.db

import org.scanamo.{Scanamo, Table}
import org.scanamo.syntax._
import com.gu.sfl.Logging
import com.gu.sfl.persistence.{DynamoSavedArticles, PersistenceConfig}
import com.gu.sfl.userdeletion.model.UserDeleteMessage
import org.scanamo.DeleteReturn.OldValue
import org.scanamo.generic.auto.genericDerivedFormat
import software.amazon.awssdk.auth.credentials.{AwsCredentialsProviderChain, EnvironmentVariableCredentialsProvider, ProfileCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient


class SflDynamoDb(persistanceConfig: PersistenceConfig) extends Logging {

  private val table = Table[DynamoSavedArticles](persistanceConfig.tableName)
  DynamoDbClient.create()
  private val client = DynamoDbClient
    .builder()
    .credentialsProvider(
      AwsCredentialsProviderChain.of(
        EnvironmentVariableCredentialsProvider.create(),
        ProfileCredentialsProvider.create("mobile"),
        )
    ).region(Region.EU_WEST_1)
    .build()
  private val scanamo = Scanamo(client)
  def deleteSavedArticleasForUser(user: UserDeleteMessage) = {
    logger.info(s"Deleting record for user id: ${user.userId}")
    val dbResponse = scanamo.exec(table.deleteAndReturn(OldValue)("userId" === user.userId))
      .fold(s"Unable to delete record for user ${user.userId}")((_) => s"Deleted record for ${user.userId}")
    logger.info(dbResponse)
  }

}
