import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.fasterxml.jackson.core.{JsonGenerator, JsonProcessingException}
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.gu.sfl.model.SavedArticle
import com.gu.sfl.lib.Jackson.mapper

@JsonSerialize(using = classOf[ArticleSerializer])
case class XSavedArticle(id: String, shortUrl: String, d: LocalDateTime, read: Boolean)

val d = LocalDateTime.now()

val art = XSavedArticle("iddxd", "shddookkrt", d, true)
val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
formatter.format(d)
class ArticleSerializer(t:Class[XSavedArticle]) extends StdSerializer[XSavedArticle](t) {

  def this() = this(null)


  @Override
  @throws(classOf[IOException])
  @throws(classOf[JsonProcessingException])
  def serialize(value: XSavedArticle, gen: JsonGenerator, serializers: SerializerProvider)
  = {
    println("hiya")
    gen.writeStartObject()
    gen.writeStringField("id", value.id)
    gen.writeStringField("short", value.shortUrl)
    val str = formatter.format(value.d)
    gen.writeStringField("date", str)
    gen.writeBooleanField("read", value.read)
    gen.writeEndObject()
  }
}
val module = new SimpleModule()
//module.addSerializer(classOf[Article], new ArticleSerializer())
//mapper.registerModule(module)
mapper.writeValueAsString(art)
