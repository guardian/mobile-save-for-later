package com.gu.sfl

import org.apache.logging.log4j.LogManager

import scala.util.{Failure, Success, Try}

trait Logging {
  val logger = LogManager.getLogger(this.getClass.getName)

  def logOnThrown[T](function: () => T, messageOnError: String = ""): T = Try(function()) match {
    case Success(value) => value
    case Failure(throwable) =>
      logger.warn(messageOnError, throwable)
      throw throwable
  }
}
