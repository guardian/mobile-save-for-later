package com.gu.sfl.lib

import java.util.concurrent.ForkJoinPool

import scala.concurrent.ExecutionContext

object Parallelism {
  implicit val largeGlobalExecutionContext: ExecutionContext = ExecutionContext.fromExecutor(new ForkJoinPool(5))
}
