package com.gu.sfl.lib

import com.gu.sfl.identity.IdentityHeaders
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class AuthHeaderParserSpec extends Specification {
  
  "parse" should {
    "Get the auth from a lowercase header" in new AuthHeaderScope {
       val headers = Map("authorization" -> "someAuth")
       parser.getIdentityHeaders(headers) mustEqual(expectedIdentityHeaders)
    }

    "Get the auth from a upperCase header" in new AuthHeaderScope {
       val headers = Map("Authorization" -> "someAuth")
       parser.getIdentityHeaders(headers) mustEqual(expectedIdentityHeaders)
    }

    "Np auth header gives returns none" in new AuthHeaderScope  {
      val headers = Map("tosh" -> "someTosh")
      parser.getIdentityHeaders(headers) mustEqual(None)
    }

  }

  trait AuthHeaderScope extends Scope {
     val parser = new AuthHeaderParser {}
     val expectedIdentityHeaders = Some(IdentityHeaders("someAuth", "Bearer application_token"))
  }

}
