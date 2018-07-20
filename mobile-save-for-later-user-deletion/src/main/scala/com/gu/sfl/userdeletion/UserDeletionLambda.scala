package com.gu.sfl.userdeletion

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.gu.sfl.Logging
import com.gu.sfl.lib.Jackson.mapper
import com.gu.sfl.persistance.PersistenceConfig
import com.gu.sfl.userdeletion.model.User
import coml.gu.sfl.userdeletion.db.SflDynamoDb

import scala.collection.JavaConverters._


object UserDeletionLambda extends Logging {

  logger.info("Hello from lambda!")

  val saveForLaterApp = Option(System.getenv("SaveForLaterApp")).getOrElse(sys.error("No main app name configured"))
  val stage = Option(System.getenv("Stage")).getOrElse(sys.error("No main stage configured"))

  val sflDyanamo = new SflDynamoDb(PersistenceConfig(saveForLaterApp, stage))

  def handler(sQSEvent: SQSEvent) {
    val messages = sQSEvent.getRecords.asScala.map(mes => mes).toList
    logger.info(s"Recieved:  ${messages.size} mesages")
    messages.map{
      m => logger.info(s"Message body: ${m.getBody}")
      val user = mapper.readValue[User](m.getBody)
      sflDyanamo.deleteSavedArticleasForUser(user)
    }

  }
}
