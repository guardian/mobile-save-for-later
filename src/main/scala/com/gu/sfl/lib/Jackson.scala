package com.gu.sfl.lib

import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object Jackson {

  val formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

  val mapper = new ObjectMapper

  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  mapper.setDateFormat(formatter)
  mapper.registerModule(DefaultScalaModule)
}
