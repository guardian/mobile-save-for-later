# Save for later

This replaces the old [syncedPrefs service](https://github.com/guardian/identity/tree/master/identity-synced-prefs) formerly provided by the [identity-api](https://github.com/guardian/identity/tree/master/identity-synced-prefs)


## Summary

The service is implemented as a pair of aws [lamba functions](https://aws.amazon.com/lambda/) set behind aws [api gateway](https://aws.amazon.com/api-gateway) which you can view and test by going to the api gateway section of the console [here](https://eu-west-1.console.aws.amazon.com/apigateway/home?region=eu-west-1#/apis) 

There's a fetch function (`/syncedPrefs/me`)(GET) and a update function (http POST) `/syncedPrefs/me/savedArticles`. Each endpoint requires a `authorisation` header containing a valid access token. This is used to retrieve the userId from the identity api. 

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
         
Here the `version` property is a timesamp and the `articles` array is all of the users currently saved articles. The version property is used to ensure that saved articles can be synced across different devices
