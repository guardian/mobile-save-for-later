package savedarticles

import java.time.LocalDateTime

import com.gu.sfl.controller.{SavedArticle, SavedArticles}
import com.gu.sfl.lib.SavedArticlesMerger
import com.gu.sfl.savedarticles.UpdateSavedArticlesImpl
import com.gu.sfl.services.IdentityService
import org.specs2.matcher.ThrownMessages
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class UpdateSavedArticlesSpec extends Specification with ThrownMessages with Mockito {

  "update should not call the identity api without the auth header present" in new Setup {

     updateSavedArticles.saveSavedArticles(Map.empty, savedArticles)
     there were no(identityService.userFromRequest()


  }

  trait Setup extends Scope {

    val savedArticles = SavedArticles("123",
      List(
        SavedArticle("id/1", "p/1", LocalDateTime.of(2018, 1, 16, 16, 30), read = true),
        SavedArticle("id/2", "p/2", LocalDateTime.of(2018, 2, 17, 17, 30), read = false)
    ))

    val identityService = mock[IdentityService]
    val articlesMerger = mock[SavedArticlesMerger]
    val updateSavedArticles = new UpdateSavedArticlesImpl(identityService, articlesMerger)
  }

}
