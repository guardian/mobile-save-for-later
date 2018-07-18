package com.gu.sfl.logging;

trait Logging {
  val logger = LogManager.getLogger(this.getClass.getName)
}
