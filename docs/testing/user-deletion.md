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

* Go to `src/main/scala/local/RunUserDeletionLambda.scala`
* Select the drop down menu to the left of the run button and select `RunUserDeletionLambda` under run configurations
* Then choose `Edit Configurations`
* Add the following environment variables:
* `SaveForLaterApp=mobile-save-for-later;Stage=CODE`
* Modify the event json at `src/main/resources/delete-event.json` to have the user id you want
* Hit the green run button