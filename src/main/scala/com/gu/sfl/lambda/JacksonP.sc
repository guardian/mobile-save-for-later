import java.text.SimpleDateFormat
import java.time.LocalDate

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.gu.sfl.save.SavedArticle

val formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

val mapper = new ObjectMapper

mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
mapper.setDateFormat(formatter)
mapper.registerModule(DefaultScalaModule)

val d = LocalDate.now()

val articles = List(
  SavedArticle(id = "/uk/politics/corbyn-knobber", date = d, shortUrl = "/p/ksjd2", read = false)
)