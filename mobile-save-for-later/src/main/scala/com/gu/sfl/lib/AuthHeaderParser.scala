package com.gu.sfl.lib

import com.gu.sfl.identity.{IdentityHeadersWithAuth, IdentityHeadersWithCookie, IdentityHeaders}
import com.gu.sfl.util.HeaderNames.Identity

trait AuthHeaderParser {
    def getIdentityHeaders(headers: Map[String, String]) : Option[IdentityHeaders] = {
      //Ios sends 'authorisation' whereas android 'Authorisation'
      val authOpt = for {
        auth <- headers.get(Identity.auth)
        isOauth = headers.contains(Identity.isOauth)
      } yield IdentityHeadersWithAuth(auth = auth, isOauth = isOauth)

      val cookieOpt = for {
        scGuUCookie <- headers.get(Identity.SCGUUCookie)
        clientAccessToken <- headers.get(Identity.accessToken)
      } yield IdentityHeadersWithCookie(scGuUCookie = scGuUCookie, accessToken = clientAccessToken)

      if (authOpt.isDefined) {
        authOpt
      } else {
        cookieOpt
      }
    }
}
