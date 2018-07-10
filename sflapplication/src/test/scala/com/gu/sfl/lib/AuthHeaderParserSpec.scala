package com.gu.sfl.lib

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import sfl.identity.IdentityHeader
import sfl.lib.AuthHeaderParser

class AuthHeaderParserSpec extends Specification {
  
  "parse" should {
    "Get the auth from a header" in new AuthHeaderScope {
       val headers = Map("authorization" -> "someAuth")
       parser.getIdentityHeaders(headers) mustEqual(expectedIdentityHeaders)
    }

    "No auth header returns none" in new AuthHeaderScope  {
      val headers = Map("tosh" -> "someTosh")
      parser.getIdentityHeaders(headers) mustEqual(None)
    }

  }

  trait AuthHeaderScope extends Scope {
     val parser = new AuthHeaderParser {}
     val expectedIdentityHeaders = Some(IdentityHeader("someAuth", "Bearer application_token"))
  }

}
