## Save for later

This replaces the old [syncedPrefs service](https://github.com/guardian/identity/tree/master/identity-synced-prefs) formerly provided by the [identity-api](https://github.com/guardian/identity/tree/master/identity-synced-prefs)


### Summary

The service is implemented as a pair of aws [lamba functions](https://aws.amazon.com/lambda/) set behind aws [api gateway](https://aws.amazon.com/api-gateway) which you can view and test by going to the api gateway section of the console [here](https://eu-west-1.console.aws.amazon.com/apigateway/home?region=eu-west-1#/apis) 

There's a fetch function (`/syncedPrefs/me`)(GET) and a update function(http POST) `/syncedPrefs/me/savedArticles`. Each endpoint requires a `authorisation` header containing a valid access token. This is used to retrieve the userId from the identity api. 

To update saved articles a json body is required: This should take the forms


         {
            "version": "1526053913596",
            "articles": [{
                "id": "world/2018/mar/08/donald-trump-north-korea-kim-jong-un-meeting-may-letter-invite-talks-nuclear-weapons",
                "shortUrl": "/p/88btx",
                "date": "2018-03-09T14:08:02Z",
                "read": false
            }, {
                "id": "football/2018/mar/12/jamie-carragher-dropped-by-danish-broadcaster-after-spitting-incident",
                "shortUrl": "/p/88qhj",
                "date": "2018-03-12T16:53:32Z",
                "read": false
            }, {
                "id": "world/2018/mar/19/europe-sharply-divided-over-vladimir-putins-re-election",
                "shortUrl": "/p/8a2m4",
                "date": "2018-03-19T16:00:42Z",
                "read": false
            }]
         }
         
Here the `version` property is a timesamp and the `articles` array is all of the users currently saved articles. The version property is used to ensure that saved articles can be synced accross different devices

### User Help Queries

Quite often userhelp will send queries that require looking up a users' record in dynamo. Usually, you'll want to either check that they have saved articles in the database or perhaps check how many they have

To query the save for later data you'll need to get a users' userId. You can use the User admin tool to do this. Ask the identity team to grant you permission to login and the url. You will then be able to get a user id from an email address. 

Once you have a userId, you can query the `mobile-save-for-later-PROD-articles` table in [Dynamo](https://eu-west-1.console.aws.amazon.com/dynamodb/home?region=eu-west-1#) and determine whether or not a user has any articles saved

Be aware that there is an upper limit of 1000 articles that can be saved per user so sometimes you might want to check how many articles make up a user's record. You can do this ( and other simple tasks ) with the following code in a scala worksheet along with the. Here, assuming that the contents of the `articles` field of the users record have been copied into a file `user.json` and saved in the same directory as the worksheet. This example prints the number of articles and the date of the most recent article for a given record.

```import java.time.{LocalDateTime, ZoneOffset}

import scala.util.{Failure, Success, Try}
import com.gu.sfl.lib.Jackson._
import com.gu.sfl.model.{SavedArticle, SavedArticles}

import scala.io.Source

implicit val localDateOrdering: Ordering[LocalDateTime] = Ordering.by(_.toEpochSecond(ZoneOffset.UTC))
implicit val ordering: Ordering[SavedArticle] = Ordering.by[SavedArticle, LocalDateTime](_.date)

def resourceJson(key: String): String = {
  val contentFile = getClass.getResource(key)
  val content = Try { Source.fromFile(key).getLines.mkString } match {
    case Success(x) => x
    case Failure(ex) =>
      println(ex)
      "{}"
  }
  content
}

val json = resourceJson("user.json")

Try(mapper.readValue[List[SavedArticle]](json)) match {
  case Success(savedArticles) =>
    val mostRecent = savedArticles.sorted.reverse.head.date
    println(s"Number of articles ${savedArticles.length}. Latest: $mostRecent ")
  case Failure(_) =>
    println("Parse error")
}
````

*NB: I found that when a user record has 350+ articles the resultant string in too long for idea to handle. 

## Testing the Apps on CODE

[Save For Later App](docs/testing/save-for-later.md)