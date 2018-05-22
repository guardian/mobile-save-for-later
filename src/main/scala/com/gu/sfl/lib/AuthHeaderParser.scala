package com.gu.sfl.lib

import com.gu.sfl.identity.IdentityHeaders
import com.gu.sfl.util.HeaderNames.Identity

trait AuthHeaderParser {
    def getIdentityHeaders(headers: Map[String, String]) : Option[IdentityHeaders] = {
      //Ios sends 'authorisation' whereas android 'Authorisation'
      val lowerHeaders = headers.map { case(key, value) => (key.toLowerCase, value)}
      for {
        auth <- lowerHeaders.get(Identity.auth)
      } yield (IdentityHeaders(auth = auth))
    }
}
