package org.github.ainr.rublepulse_bot

import cats.effect.kernel.Sync
import cats.effect.std.Supervisor
import cats.effect.{IO, Resource}
import org.github.ainr.configurations.Configurations
import org.github.ainr.context.{Context, TrackingIdGen}
import org.github.ainr.graphs.Graphs
import org.github.ainr.kafka.Consumer
import org.github.ainr.logger.CustomizedLogger
import org.github.ainr.telegram.BotModule
import org.http4s
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.log4cats.LoggerName
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.language.postfixOps

object RublePulseApp {

  private final case class Resources(
    supervisor: Supervisor[IO],
    httpClient: http4s.client.Client[IO]
  )

  private def resources: Resource[IO, Resources] = for {
    supervisor <- Supervisor[IO]
    httpClient <- BlazeClientBuilder[IO].resource
  } yield Resources(supervisor, httpClient)

  def run: IO[Unit] = Configurations.load[IO].flatMap {
    configs => for {
      context <- Context.make
      trackingIdGen = TrackingIdGen(context)
      logger = CustomizedLogger(
        Slf4jLogger.getLogger[IO](Sync[IO], LoggerName("RublePulseApp")),
        context
      )
      _ <- resources.use {
        resource => for {
          _ <- logger.info("Resources ok")
          botModule = BotModule(configs.telegram, resource.httpClient)(context, logger, trackingIdGen)
          graphs = Graphs[IO]()
          consumer <- Consumer[IO](configs.consumer, logger)
          rublePulseService = RublePulseService(configs.rublePulseConfig, consumer, botModule.bot, logger, graphs)
          _ <- resource.supervisor.supervise(rublePulseService.start)
          _ <- logger.info("App started")
          _ <- botModule.bot.start()
        } yield ()
      }
    } yield ()
  }
}
