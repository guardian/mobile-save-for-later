import javax.swing.event.DocumentEvent.EventType

import com.gu.sfl.lib.Jackson.mapper

val body = """{ "Type": "Notification", "MessageId": "ff469a70-d7fe-5d63-8b1d-c9a42b689fa5", "TopicArn": "arn:aws:sns:eu-west-1:942464564246:com-gu-identity-account-deletions-CODE", "Subject": "Account deletion event", "Message": "{\"userId\":\"100001539\",\"eventType\":\"DELETE\"}", "Timestamp": "2018-07-27T10:56:22.704Z", "SignatureVersion": "1", "Signature": "GZ0IGpHaiAtjkK5mECPQindJLmK9koRyNxkaicmoXi4XPecgvlpji5sC5W+864pjqrwclEiiP7TBlvK7Sv3QHZT/4Mog79+em7MWY2tPMENY/XneBRNtysTz142T0mp12VLirswynZzbMkjGXJCJjO9brkaTcc+8kDG6zn4QwId2b+nyAY8aGmuG+qyxDMVNXOUISPtXHjD8xlDCmQJZZwmeF6XHiUl+nQI+fpuzLJd/5DgIr/ZJ2ve7WvNLh9agMGEg4ua/rEffJpo3JxYzSztuUKRFwOAsxI/M6E7svidVRVjnnOvJXqNn+4mOTtex3ONa3ZGp9mNT2nFMgfUR5w==", "SigningCertURL": "https://sns.eu-west-1.amazonaws.com/SimpleNotificationService-eaea6120e66ea12e88dcd8bcbddca752.pem", "UnsubscribeURL": "https://sns.eu-west-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-1:942464564246:com-gu-identity-account-deletions-CODE:0cd6be65-7f7d-4e4c-a174-8dcdef1f5bd5" }"""
case class User(userId: String, eventType: String)

val node = mapper.readTree(body)
val userDetele = node.get("Message").textValue()
val user = mapper.readValue[User](userDetele)