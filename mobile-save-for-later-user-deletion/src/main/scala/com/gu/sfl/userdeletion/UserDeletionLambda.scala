package com.gu.sfl.userdeletion

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.gu.sfl.Logging
import com.gu.sfl.lib.Jackson.mapper
import com.gu.sfl.persistence.PersistenceConfig
import com.gu.sfl.userdeletion.model.UserDeleteMessage
import coml.gu.sfl.userdeletion.db.SflDynamoDb

import scala.collection.JavaConverters._

object UserDeletionLambda extends Logging {

  logger.info("Lambda loading")

  val saveForLaterApp = Option(System.getenv("SaveForLaterApp")).getOrElse(sys.error("No main app name configured"))
  val stage = Option(System.getenv("Stage")).getOrElse(sys.error("No main stage configured"))

  val sflDyanamo = new SflDynamoDb(PersistenceConfig(saveForLaterApp, stage))

  def handler(sQSEvent: SQSEvent) {
    val messages = sQSEvent.getRecords.asScala.map(mes => mes).toList
    logger.info(s"Recieved:  ${messages.size} mesages")
    messages.map{
      m =>
        val body = m.getBody
        logger.debug(s"Message body: ${body}")
        val node = mapper.readTree(body)
        val user = mapper.readValue[UserDeleteMessage](node.get("Message").asText())
        sflDyanamo.deleteSavedArticleasForUser(user)
    }
  }
}
