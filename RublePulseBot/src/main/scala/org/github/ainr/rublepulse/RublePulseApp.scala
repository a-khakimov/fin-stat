package org.github.ainr.rublepulse

import cats.effect.kernel.Sync
import cats.effect.std.Supervisor
import cats.effect.{IO, Resource}
import dev.profunktor.redis4cats.connection.RedisClient
import dev.profunktor.redis4cats.data.RedisChannel
import org.github.ainr.configurations.Configurations
import org.github.ainr.context.{Context, TrackingIdGen}
import org.github.ainr.graphs.Graphs
import org.github.ainr.logger.CustomizedLogger
import org.github.ainr.redis.consumer.{Consumer, RedisConsumer}
import dev.profunktor.redis4cats.effect.Log.Stdout._
import org.github.ainr.telegram.BotModule
import org.github.ainr.telegram.reaction.SendText
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
    lastPriceEventsConsumer: Consumer[IO, LastPriceEvent]
  )

  private def resources: Resource[IO, Resources] = for {
    supervisor <- Supervisor[IO]
    httpClient <- BlazeClientBuilder[IO].resource
    redisClient <- RedisClient[IO].from("redis://localhost")
    lastPriceEventsConsumer <- {
      val channel = RedisChannel("last_price_events")
      RedisConsumer[IO, LastPriceEvent](redisClient, channel)
    }
  } yield Resources(supervisor, httpClient, lastPriceEventsConsumer)

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
          rublePulseService = RublePulseService(
            configs.rublePulseConfig, resource.lastPriceEventsConsumer,
            botModule.bot, logger, graphs
          )
          _ <- resource.supervisor.supervise(rublePulseService.start)
          _ <- botModule.bot.interpret(SendText(ChatIntId(174861972), "App started") :: Nil)
          _ <- logger.info("App started")
          _ <- botModule.bot.start()
        } yield ()
      }
    } yield ()
  }
}
