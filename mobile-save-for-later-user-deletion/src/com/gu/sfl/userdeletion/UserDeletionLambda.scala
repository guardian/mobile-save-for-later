package com.gu.sfl.userdeletion

import com.amazonaws.services.sqs.model.Message

object UserDeletionLambda extends Logging {

  def handler(message: Message): Unit =  {
    logger.info(s"got message ${message.getBody}")
  }
}
