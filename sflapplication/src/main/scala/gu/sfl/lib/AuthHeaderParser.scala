package sfl.lib

import sfl.identity.IdentityHeader
import sfl.util.HeaderNames.Identity

trait AuthHeaderParser {
    def getIdentityHeaders(headers: Map[String, String]) : Option[IdentityHeader] = {
      //Ios sends 'authorisation' whereas android 'Authorisation'
      for {
        auth <- headers.get(Identity.auth)
      } yield (IdentityHeader(auth = auth))
    }
}
