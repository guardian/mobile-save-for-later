### Testing User Deletion on CODE

### Testing via the website
* Sign in to the website on [CODE](https://code.dev-theguardian.com/uk)
* Under My Account -> Settings scroll down and hit Delete Account
* Check the cloudwatch output in the [user delete lambda](https://eu-west-1.console.aws.amazon.com/lambda/home?region=eu-west-1#/functions/mobile-save-for-later-user-deletion-CODE?tab=monitoring)

### Testing the lambda directly

* Go to the [user delete lambda](https://eu-west-1.console.aws.amazon.com/lambda/home?region=eu-west-1#/functions/mobile-save-for-later-user-deletion-CODE?tab=monitoring)
* Run the test tab with this message structure:
```agsl
{
  "Records": [
    {
      "messageId": "19dd0b57-b21e-4ac1-bd88-01bbb068cb78",
      "receiptHandle": "MessageReceiptHandle",
      "body": "{\"Message\": \"{\\\"userId\\\":\\\"200126633\\\",\\\"eventType\\\":\\\"DELETE\\\"}\"}",
      "attributes": {
        "ApproximateReceiveCount": "1",
        "SentTimestamp": "1523232000000",
        "SenderId": "123456789012",
        "ApproximateFirstReceiveTimestamp": "1523232000001"
      },
      "messageAttributes": {},
      "md5OfBody": "{{{md5_of_body}}}",
      "eventSource": "aws:sqs",
      "eventSourceARN": "arn:aws:sqs:us-east-1:123456789012:MyQueue",
      "awsRegion": "us-east-1"
    }
  ]
}
```

## Testing locally

### Pre-requisites
1) Mobile credentials from [Janus](https://janus.gutools.co.uk/login)

* Go to the drop down menu between the build and play button and select `Edit Configurations`
* Make sure `mobile-save-for-later-user-deletion` is selected for the module dropdown
* Check that `local.RunUserDeletionLambda` is selected in the Main Module field
* Add the following environment variables:
  `App=mobile-save-for-later;IdentityApiHost=https://id.code.dev-guardianapis.com;IdentityOktaAudience=https://profile.code.dev-theguardian.com/;IdentityOktaIssuerUrl=https://profile.code.dev-theguardian.com/oauth2/aus3v9gla95Toj0EE0x7;Stage=CODE;SavedArticleLimit=100;SaveForLaterApp=mobile-save-for-later`
* Modify `src/main/resources/delete-event.json` to have the user id you want (inside `"body"` > `"message"` > `"userId"` value)
* Hit the green run button
* If successful, you should see `Deleted record for <user-id>` in the terminal (unless the user already doesn't exist in the database, in which case `Unable to delete record for user <user-id>`).
* Alternatively you can check in AWS Console DynamoDB table `mobile-save-for-later-CODE-articles` that the corresponding user record is gone
