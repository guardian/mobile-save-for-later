package local

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.gu.sfl.userdeletion.UserDeletionLambda

import scala.io.Source

object RunUserDeletionLambda extends App{

  val json = Source.fromResource("delete-event.json").getLines().mkString
  val mapper = new ObjectMapper()

  val sQSEvent = mapper.readValue(json, classOf[SQSEvent])
  UserDeletionLambda.handler(sQSEvent)

}
