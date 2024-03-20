package com.gu.sfl.lib

import java.text.SimpleDateFormat

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

object Jackson {

  val formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

  val mapper = new ObjectMapper() with ScalaObjectMapper

  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  mapper.setDateFormat(formatter)
  mapper.registerModule(DefaultScalaModule)
  mapper.registerModule(new JavaTimeModule())
}
