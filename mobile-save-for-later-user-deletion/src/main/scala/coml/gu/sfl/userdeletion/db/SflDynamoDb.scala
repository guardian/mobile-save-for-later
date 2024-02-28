package coml.gu.sfl.userdeletion.db

import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import org.scanamo.{DynamoFormat, DynamoReadError, DynamoValue, Scanamo, Table}
import org.scanamo.syntax._
import com.gu.sfl.Logging
import com.gu.sfl.model.SavedArticles
import com.gu.sfl.persistence.{DynamoSavedArticles, PersistenceConfig}
import com.gu.sfl.userdeletion.model.UserDeleteMessage

class SflDynamoDb(persistanceConfig: PersistenceConfig) extends Logging {
  implicit val formatcom: DynamoFormat[com.gu.sfl.persistence.DynamoSavedArticles] = new DynamoFormat[DynamoSavedArticles] {
    override def read(av: DynamoValue): Either[DynamoReadError, DynamoSavedArticles] = Right(DynamoSavedArticles("uasd", SavedArticles("asd", Nil)))

    override def write(t: DynamoSavedArticles): DynamoValue = DynamoValue.fromString("")
  }

  private val table = Table[DynamoSavedArticles](persistanceConfig.tableName)
  private val client = DynamoDbClient.builder().build()
  private val scanamo = Scanamo(client)


  def deleteSavedArticleasForUser(user: UserDeleteMessage) = {
    logger.info(s"Deleting record for user id: ${user.userId}")
    logger.info(Option(scanamo.exec(table.delete("userId" -> user.userId))).fold(s"Unable to delete record for user ${user.userId}")(_ => s"Deleted record for ${user.userId}") )
  }

}
