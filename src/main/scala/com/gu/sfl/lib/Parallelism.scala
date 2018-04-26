package com.gu.sfl

import java.util.concurrent.ForkJoinPool

import scala.concurrent.ExecutionContext

object Parallelism {
  val largeGlobalExecutionContext: ExecutionContext = ExecutionContext.fromExecutor(new ForkJoinPool(25))
}
