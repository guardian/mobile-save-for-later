package com.gu.sfl.services

import com.gu.sfl.Logging
import com.gu.sfl.lambda.LambdaRequest

case class IdentityConfig(identityApiUrl: String)

trait IdentityService {

  def userFromRequest(lambdaRequest: LambdaRequest) : Option[String] //Will be Future[Option[UserId]]]

}

class IdentityServiceImpl(identityConfig: IdentityConfig) extends IdentityService with Logging {

  //TODO - transfer the tokenHeader to a user id and return a future
  override def userFromRequest(lambdaRequest: LambdaRequest): Option[String] = lambdaRequest.headers.get("userId")
}
