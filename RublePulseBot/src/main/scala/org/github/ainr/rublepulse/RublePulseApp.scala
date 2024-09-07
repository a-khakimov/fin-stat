package org.github.ainr.rublepulse

import cats.effect.std.Supervisor
import cats.effect.{IO, Resource, Sync}
import doobie.hikari.HikariTransactor
import org.github.ainr.configurations.Configurations
import org.github.ainr.context.{Context, TrackingIdGen}
import org.github.ainr.db.Database
import org.github.ainr.graphs.Graphs
import org.github.ainr.logger.CustomizedLogger
import org.github.ainr.telegram.BotModule
import org.github.ainr.telegram.reaction.SendText
import org.github.ainr.tinvest.TinvestModule
import org.http4s
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.log4cats.LoggerName
import org.typelevel.log4cats.slf4j.Slf4jLogger
import telegramium.bots.ChatIntId

import scala.language.postfixOps

object RublePulseApp {

  private final case class Resources(
    supervisor: Supervisor[IO],
    httpClient: http4s.client.Client[IO],
    transactor: HikariTransactor[IO],
  )

  private def buildResources(configs: Configurations): Resource[IO, Resources] = for {
    supervisor <- Supervisor[IO]
    httpClient <- BlazeClientBuilder[IO].resource
    transactor <- Database.makePostgresResource[IO](configs.postgres)
  } yield Resources(supervisor, httpClient, transactor)

  def run: IO[Unit] = for {
    context <- Context.make
    trackingIdGen = TrackingIdGen(context)
    logger = CustomizedLogger(Slf4jLogger.getLogger[IO](Sync[IO], LoggerName("RublePulseApp")), context)
    configs <- Configurations.load[IO]
    _ <- {
      val app = for {
        resources <- buildResources(configs)
        _ <- Resource.eval(logger.info("Resources ok"))
        botModule = BotModule(configs.telegram, resources.httpClient)(context, logger, trackingIdGen)
        graphs <- Graphs[IO]()
        rublePulseService = RublePulseService(
          configs.rublePulseConfig,
          botModule.bot, logger, graphs
        )
        _ <- Resource.eval(resources.supervisor.supervise(rublePulseService.start))
        _ <- Resource.eval(botModule.bot.interpret(SendText(ChatIntId(174861972), "App started")))
        _ <- TinvestModule.make[IO](configs.tinvestConfig, resources.supervisor, logger)
        _ <- Resource.eval(logger.info("App started"))
        _ <- Resource.eval(botModule.bot.start())
      } yield ()

      app.useForever.onError {
        cause => logger.error(cause)("Error starting application")
      }
    }
  } yield ()
}
