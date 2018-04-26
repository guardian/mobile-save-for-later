def maybeHeades: Option[(String, String)] = for {
  auth <- Some("auth")
  token <- None
} yield (auth, token)

case class Box(id: String)

for {
  (auth, token) <- maybeHeades
} yield Box("yowzs")
