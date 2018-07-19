package com.gu.sfl.userdeletion

import com.amazonaws.services.sqs.model.Message
import com.gu.sfl.logging.Logging

object UserDeletionLambda extends Logging {

  logger.info("Hello from lambda!")

  def handler(message: Message): Unit =  {
    logger.info(s"got message ${message.getBody}")
  }
}
