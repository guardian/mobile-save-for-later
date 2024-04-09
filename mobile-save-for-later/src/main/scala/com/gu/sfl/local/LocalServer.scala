package com.gu.sfl.local

import cats.data.Kleisli
import cats.effect.{ExitCode, IO, IOApp}
import com.gu.sfl.lambda.{FetchArticlesLambda, LambdaRequest, LambdaResponse}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.io._
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.Router
import org.http4s.{HttpRoutes, Request, Response}

object LocalServer extends IOApp {

  val route: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case request @ GET -> Root / "syncedPrefs" / "me" =>
      val lambsResFt = FetchArticlesLambda.fetchArticlesController.apply(LambdaRequest.fromRequest(request))
      IO.fromFuture(IO(lambsResFt)).map(LambdaResponse.toHttp4sRes)

  }

  val httpApp: Kleisli[IO, Request[IO], Response[IO]] = Router(
    "/" -> route
  ).orNotFound

  import scala.concurrent.ExecutionContext.global
  override def run(args: List[String]) =
    BlazeServerBuilder[IO](global)
      .bindHttp(8080, "localhost")
      .withHttpApp(httpApp)
      .serve
      .compile.drain.as(ExitCode.Success)
}