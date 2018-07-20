package com.gu.sfl.userdeletion

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.sqs.model.Message
import com.gu.sfl.logging.Logging
import scala.collection.JavaConverters._


object UserDeletionLambda extends Logging {

  logger.info("Hello from lambda!")

  /*def handler(message: List[Message]): Unit =  {
    message.foreach( m => logger.info(s"got message - body:  ${m.getBody()} ${m.getMessageId()}")
  */

  def handler(sQSEvent: SQSEvent) {
    val messages = sQSEvent.getRecords.asScala.map(mes => mes).toList
    logger.info(s"Recieved:  ${messages.size} mesages")
    messages.map{
      m => logger.info(s"Message body: ${m.getBody}")
    }

  }
}
