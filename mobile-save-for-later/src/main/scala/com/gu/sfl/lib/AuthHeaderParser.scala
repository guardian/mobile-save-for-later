package com.gu.sfl.lib

import com.gu.sfl.identity.IdentityHeader
import com.gu.sfl.util.HeaderNames.Identity

trait AuthHeaderParser {
    def getIdentityHeaders(headers: Map[String, String]) : Option[IdentityHeader] = {
      //Ios sends 'authorisation' whereas android 'Authorisation'
      for {
        auth <- headers.get(Identity.auth)
        isOauth = headers.contains(Identity.oauth)
      } yield (IdentityHeader(auth = auth, isOauth = isOauth))
    }
}
