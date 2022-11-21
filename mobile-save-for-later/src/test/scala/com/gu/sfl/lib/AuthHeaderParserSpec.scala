package com.gu.sfl.lib

import com.gu.sfl.identity.IdentityHeader
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

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

    "Get 'isOauth' flag if header is set" in new AuthHeaderScope {
      val headers = Map("authorization" -> "someAuth", "X-GU-Is-OAuth" -> "true")
      parser.getIdentityHeaders(headers) mustEqual (expectedOauthHeaders)
    }

  }

  trait AuthHeaderScope extends Scope {
     val parser = new AuthHeaderParser {}
     val expectedIdentityHeaders = Some(IdentityHeader("someAuth", "Bearer application_token"))
     val expectedOauthHeaders = Some(IdentityHeader("someAuth", "Bearer application_token", true))
  }

}
