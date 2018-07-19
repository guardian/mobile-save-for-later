package com.gu.sfl.logging

import org.apache.logging.log4j.LogManager
;

trait Logging {
  val logger = LogManager.getLogger(this.getClass.getName)
}
