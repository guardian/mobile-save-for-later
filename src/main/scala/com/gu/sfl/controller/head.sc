val headers: Option[Map[String, String]] = Some(Map("Auth" -> "auth", "Toke" -> "toke"))

val a = headers.map{ h => h.map {case (key, value) => (key.toLowerCase, value)}}.getOrElse(Map.empty)
val b = headers.getOrElse(Map.empty)
val c = headers.map(_.map{ case (k,v) => (k.toLowerCase(), v)}).getOrElse(Map.empty)