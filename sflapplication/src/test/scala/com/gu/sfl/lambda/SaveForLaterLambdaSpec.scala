package com.gu.sfl.lambda

import org.specs2.mutable.Specification
import sfl.lambda.SaveArticlesLambda

class SaveForLaterLambdaSpec extends Specification{
  "SaveForLaterLambda" should {
      "initialise" in {
          new SaveArticlesLambda() must throwA[IllegalStateException]
      }
  }
}
