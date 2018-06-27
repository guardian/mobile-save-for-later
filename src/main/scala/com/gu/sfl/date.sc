import java.time.{LocalDateTime, ZonedDateTime}
import java.time.format.DateTimeFormatterBuilder

import com.gu.sfl.lib.Jackson.mapper
import com.gu.sfl.model.SavedArticle

println("Hi")
val d = 12
/*
val d = ZonedDateTime.now()//.format(new DateTimeFormatterBuilder().parseCaseInsensitive().appendInstant(0).toFormatter)
case class Article(id: String, short: String, d: ZonedDateTime, read: Boolean)

val art = Article("id", "shookkrt", d, true )
mapper.writeValueAsString(art)*/
