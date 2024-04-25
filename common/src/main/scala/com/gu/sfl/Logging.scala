package com.gu.sfl

import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}

trait Logging {
  val logger: Logger = LoggerFactory.getLogger(this.getClass.getName)

  def logOnThrown[T](function: () => T, messageOnError: String = ""): T = Try(function()) match {
    case Success(value) => value
    case Failure(throwable) =>
      logger.warn(messageOnError, throwable)
      throw throwable
  }
}
