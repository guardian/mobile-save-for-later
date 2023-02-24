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
      val headers = Map("cookie" -> "SC_GU_U=somecookie")
      parser.getIdentityHeaders(headers) mustEqual (expectedIdentityCookies)
    }

    "No scGuU Cookie returns none" in new AuthHeaderScope {
      val headers = Map("cookie" -> "")
      parser.getIdentityHeaders(headers) mustEqual (None)
    }

    "Get 'isOauth' flag if header is set" in new AuthHeaderScope {
      val headers = Map("authorization" -> "someAuth", "x-gu-is-oauth" -> "true")
      parser.getIdentityHeaders(headers) mustEqual (expectedOauthHeaders)
    }

  }

  trait AuthHeaderScope extends Scope {
    val parser = new AuthHeaderParser {}
    val expectedIdentityHeaders = Some(IdentityHeadersWithAuth("someAuth"))
    val expectedIdentityCookies = Some(IdentityHeadersWithCookie("SC_GU_U=somecookie"))
    val expectedOauthHeaders = Some(IdentityHeadersWithAuth("someAuth", isOauth = true))

  }
}

