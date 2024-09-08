package org.github.ainr

import cats.effect.std.Supervisor
import cats.effect.{IO, Resource, Sync}
import doobie.Transactor
import org.github.ainr.configurations.Configurations
import org.github.ainr.context.{Context, TrackingIdGen}
import org.github.ainr.db.Database
import org.github.ainr.db.repo.TinvestRepository
import org.github.ainr.graphs.Graphs
import org.github.ainr.logger.CustomizedLogger
import org.github.ainr.rublepulse.RublePulseModule
import org.github.ainr.telegram.BotModule
import org.github.ainr.telegram.reaction.SendText
import org.http4s
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.log4cats.LoggerName
import org.typelevel.log4cats.slf4j.Slf4jLogger
import telegramium.bots.ChatIntId

import scala.language.postfixOps

object App {

  private final case class Resources(
    supervisor: Supervisor[IO],
    httpClient: http4s.client.Client[IO],
    transactor: Transactor[IO],
  )

  private def buildResources(configs: Configurations): Resource[IO, Resources] = for {
    supervisor <- Supervisor[IO]
    httpClient <- BlazeClientBuilder[IO].resource
    transactor <- Database.makePostgresResource[IO](configs.postgres)
  } yield Resources(supervisor, httpClient, transactor)

  def run: IO[Unit] = for {
    context <- Context.make
    trackingIdGen = TrackingIdGen(context)
    logger = CustomizedLogger.make[IO](Slf4jLogger.getLogger[IO](Sync[IO], LoggerName("RublePulseApp")), context)
    configs <- Configurations.load[IO]
    _ <- {
      implicit val lggr: CustomizedLogger[IO] = logger
      val app = for {
        resources <- buildResources(configs)
        _ <- Resource.eval(logger.info("Resources ok"))
        botModule = BotModule(configs.telegram, resources.httpClient)(context, logger, trackingIdGen)
        graphs <- Graphs[IO]()
        tinvestRepository <- TinvestRepository.make[IO](resources.transactor, logger)
        _ <- RublePulseModule.make[IO](configs.rublePulseConfig, resources.supervisor, graphs, botModule.bot, tinvestRepository)
        //_ <- TinvestModule.make[IO](configs.tinvestConfig, configs.lastPriceSubscriptionsConfig, resources.supervisor, logger, tinvestRepository)
        _ <- Resource.eval(botModule.bot.interpret(SendText(ChatIntId(174861972), "App started")))
        _ <- Resource.eval(logger.info("App started"))
        _ <- Resource.eval(botModule.bot.start())
      } yield ()

      app.useForever.onError {
        cause => logger.error(cause)("Error starting application")
      }
    }
  } yield ()
}
