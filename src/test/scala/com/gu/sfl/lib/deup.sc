import java.time.LocalDateTime

import com.gu.sfl.model.SavedArticle

val l = List(
  SavedArticle("id/1", "p/1", LocalDateTime.of(2018, 1, 16, 16, 30), read = true),
  SavedArticle("id/2", "p/2", LocalDateTime.of(2018, 2, 17, 17, 30), read = false),
  SavedArticle("id/3", "p/3", LocalDateTime.of(2018, 3, 18, 18, 30), read = true),
  SavedArticle("id/3", "p/3", LocalDateTime.of(2018, 3, 18, 18, 30), read = true),
  SavedArticle("id/3", "p/3", LocalDateTime.of(2018, 3, 18, 18, 30), read = true),
  SavedArticle("id/3", "p/3", LocalDateTime.of(2018, 3, 18, 18, 30), read = true),
  SavedArticle("id/3", "p/3", LocalDateTime.of(2018, 3, 18, 18, 30), read = true),
  SavedArticle("id/4", "p/4", LocalDateTime.of(2018, 4, 19, 19, 30), read = true)
).toSet.toList

l.size
