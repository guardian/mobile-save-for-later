package com.gu.sfl.lib

import com.gu.sfl.identity.{IdentityHeadersWithAuth, IdentityHeadersWithCookie}
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class AuthHeaderParserSpec extends Specification {

  "parse" should {
    "Get the auth from a header" in new AuthHeaderScope {
      val headers = Map("authorization" -> "someAuth")
      parser.getIdentityHeaders(headers) mustEqual (expectedIdentityHeaders)
    }

    "No auth header returns none" in new AuthHeaderScope {
      val headers = Map("tosh" -> "someTosh")
      parser.getIdentityHeaders(headers) mustEqual (None)
    }

    "Get the scGuU Cookie from the cookie header" in new AuthHeaderScope {
      val headers = Map("x-gu-id-fowarded-sc-gu-u" -> "somecookie",
        "x-gu-id-client-access-token" -> "Bearer token")
      parser.getIdentityHeaders(headers) mustEqual (expectedIdentityCookies)
    }

    "Get 'isOauth' flag if header is set" in new AuthHeaderScope {
      val headers = Map("authorization" -> "someAuth", "x-gu-is-oauth" -> "true")
      parser.getIdentityHeaders(headers) mustEqual (expectedOauthHeaders)
    }

  }

  trait AuthHeaderScope extends Scope {
    val parser = new AuthHeaderParser {}
    val expectedIdentityHeaders = Some(IdentityHeadersWithAuth("someAuth"))
    val expectedIdentityCookies = Some(IdentityHeadersWithCookie("somecookie", "Bearer token"))
    val expectedOauthHeaders = Some(IdentityHeadersWithAuth("someAuth", isOauth = true))

  }
}

