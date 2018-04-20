package com.gu.sfl.services

import com.gu.identity.client.IdentityApiClient
import com.gu.sfl.Logging
import com.gu.sfl.lambda.LambdaRequest
import org.apache.commons.httpclient.HttpClient

case class IdentityConfig(identityApiUrl: String)

trait IdentityService {

  def userFromRequest(lambdaRequest: LambdaRequest) : Option[String] //Will be Future[Option[UserId]]]
}

class IdentityServiceImpl(identityConfig: IdentityConfig) extends IdentityService with Logging {

  private val identityClientKey="MIIDOjCCAi0GByqGSM44BAEwggIgAoIBAQD4gSsAjnc7RF4liv95iS+h/LWkaRdqgVwwt8lk46UxD25SNn9w82uosczhDP2645F6ppGoewKQAO3CvXLqjRrJiV28RRSStOZhUbSgjfZbl8gg1XMHvTQRS1KtK5jgsGWpxUXxEgvPYOglsgB7XWo+OmWxntWGWgHeKjoxgg7bDe9fJHz6zblUqBu92i0+M/X5MWGrseuVn6AKkaKzrlYt5wHuzeVn07z09qjtFEPhI882pZ6o9je7nuKylZ+bOCvXRR49bZDb1te7evIK4twWPcRlh8jB6jJL/DsQWjtjvweyZD6PIGV0KxQBtxXQfG67jDzjcXuBxU4mw7VePqPhAhUAv4GmHccfqMVu+J8mXjcXGFj87hMCggEAIuPY5tVgZ8yWlHRMCBRyb9LL5OXxDw4mJXNqs1ykQ+BGY3oBoyFKuLWiEjjfLB10WHrso1iDi3ELfokvPsOKw8EEf18NactJuxrmyTRKObizoJG2Pekpwd/HoVRNJEBgCwrBZk6NouieFrzqnxlZ83gMvwa1iYOoKHNxBBU+8NRs8uSsjzocWhfnaX0x+62RyTfohvq42z6Anwzx7wlR45jNlu/4QCWWoJCUpPOawzSd545MfH0VYX5q4QT2KfxV3KB8y/3St6xocJDgtgX3Sb51tcMJU3710U+82iL6cxQkw5a/GYew7X8atUyLMaqHCX7ol0w6iMzxbeMGEM+fWAOCAQUAAoIBAAyTZaS/n9vhES/BLJicQmzqjfTO50OBJ/EKwxdoAgtrtHvVEfSIUSvYFnxdnk1znBvCzA+11xczsotyYx3BYd58Lpfncgz0Tk+q0aYNv9xt6K1fH2Xbab3TK7LIctMbIfCmr8gW7uaaOjrjuQUtkCHAS2/NwX+Vsh8fsgHEkcYaFf6hkU8+QtmZImTMUrQTq8MIblEQlk8UEEhcA3H/xUvsr9K6EZ6RuM9g9w64dXBvPto7sDYObjBC7uYufHviTZ7o2e3M9RS1GF4RJ+hR7r2TXiIAXZ79OXNdui8P+xPjKMJBRQR2fhLaCTgvhYmqDuERD0bKCttDM6zditsdjNs="
  private val httpClient = new HttpClient
  val token = "Bearer 4aad37356f752b84fd78da27776130103795fc3197bfb993e53a89e35782fdc4"
  val client = new IdentityApiClient("https://id.guardianapis.com", httpClient, identityClientKey)

  //TODO - transfer the tokenHeader to a user id and return a future
  override def userFromRequest(lambdaRequest: LambdaRequest): Option[String] = {
    val userId = lambdaRequest.headers.get("userId")
    logger.debug(s"UserId: $userId")
    Option(client.extractUserDataFromToken(token, "iphone")) match {
      case Some(u) =>
        logger.info(s"User; ${u.id}")
        Some("1234")
      case _  =>
        logger.info("None")
        None
    }
  }
}
