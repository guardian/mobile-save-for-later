package com.gu.sfl.userdeletion

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.sqs.model.Message
import com.gu.sfl.logging.Logging
import com.gu.sfl.userdeletion.lib.Jackson.mapper
import com.gu.sfl.userdeletion.model.User

import scala.collection.JavaConverters._


object UserDeletionLambda extends Logging {

  logger.info("Hello from lambda!")

  //SaveForLaterApp
  val saveForLaterApp = Option(System.getenv("SaveForLaterApp")).getOrElse(sys.error("No main app name configured"))
  val stage = Option(System.getenv("Stage")).getOrElse(sys.error("No main stage configured"))

  val sflDyanamo = new SflDynamoDb(PersistanceConfig(saveForLaterApp, stage))

  def handler(sQSEvent: SQSEvent) {
    val messages = sQSEvent.getRecords.asScala.map(mes => mes).toList
    logger.info(s"Recieved:  ${messages.size} mesages")
    messages.map{
      m => logger.info(s"Message body: ${m.getBody}")
      val user = mapper[User](m.getBody)
      sflDyanamo.deleteSavedArticleasForUser(user)
    }

  }
}
