package com.gu.sfl.lambda

import org.specs2.mutable.Specification

class SaveForLaterLambdaSpec extends Specification{
  "SaveForLaterLambda" should {
      "initialise" in {
          new SaveArticlesLambda() must throwA[NullPointerException]
      }
  }
}
