package com.gu.sfl.local

import cats.data.Kleisli
import cats.effect.{ExitCode, IO, IOApp}
import com.gu.sfl.lambda.{FetchArticlesLambda, LambdaRequest, LambdaResponse, SaveArticlesLambda}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.io._
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.Router
import org.http4s.{HttpRoutes, Request, Response}

import scala.concurrent.Future

object LocalServer extends IOApp {

  val route: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case request @ GET -> Root / "syncedPrefs" / "me" =>
     handle(request, FetchArticlesLambda.fetchArticlesController.apply)

    case request @ POST -> Root / "syncedPrefs" / "me" / "savedArticles" =>
      handle(request, SaveArticlesLambda.saveArticlesController.apply)
  }

  val httpApp: Kleisli[IO, Request[IO], Response[IO]] = Router(
    "/" -> route
  ).orNotFound

  import scala.concurrent.ExecutionContext.global
  override def run(args: List[String]) =
    BlazeServerBuilder[IO](global)
      .bindHttp(8888, "localhost")
      .withHttpApp(httpApp)
      .serve
      .compile.drain.as(ExitCode.Success)

  def handle(request: Request[IO], controller: LambdaRequest => Future[LambdaResponse]): IO[Response[IO]] = {
    IO.fromFuture(IO(controller(LambdaRequest.fromHttp4sRequest(request)))).map(LambdaResponse.toHttp4sRes)
  }
}