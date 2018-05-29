package com.gu.sfl.lib

import com.gu.sfl.identity.IdentityHeader
import com.gu.sfl.util.HeaderNames.Identity

trait AuthHeaderParser {
    def getIdentityHeaders(headers: Map[String, String]) : Option[IdentityHeader] = {
      //Ios sends 'authorisation' whereas android 'Authorisation'
      val lowerHeaders = headers.map { case(key, value) => (key.toLowerCase, value)}
      for {
        auth <- lowerHeaders.get(Identity.auth)
      } yield (IdentityHeader(auth = auth))
    }
}
