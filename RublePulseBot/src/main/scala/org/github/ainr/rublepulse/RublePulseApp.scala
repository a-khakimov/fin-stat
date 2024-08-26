package org.github.ainr.rublepulse

import cats.effect.kernel.Sync
import cats.effect.std.Supervisor
import cats.effect.{IO, Resource}
import cats.syntax.all._
import fs2.grpc.syntax.all._
import io.grpc.ManagedChannel
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import org.github.ainr.configurations.Configurations
import org.github.ainr.context.{Context, TrackingIdGen}
import org.github.ainr.graphs.Graphs
import org.github.ainr.logger.CustomizedLogger
import org.github.ainr.telegram.BotModule
import org.github.ainr.telegram.reaction.SendText
import org.github.ainr.tinvest.AuthMetadata
import org.http4s
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.log4cats.LoggerName
import org.typelevel.log4cats.slf4j.Slf4jLogger
import ru.tinkoff.piapi.contract.v1.marketdata._
import telegramium.bots.ChatIntId

import scala.language.postfixOps

object RublePulseApp {

  private final case class Resources(
    supervisor: Supervisor[IO],
    httpClient: http4s.client.Client[IO],
    managedChannel: ManagedChannel,
    marketDataStreamService: MarketDataStreamServiceFs2Grpc[IO, io.grpc.Metadata]
  )

  private def resources: Resource[IO, Resources] = for {
    supervisor <- Supervisor[IO]
    httpClient <- BlazeClientBuilder[IO].resource
    managedChannel <- NettyChannelBuilder.forAddress("invest-public-api.tinkoff.ru", 443).resource[IO]
    marketDataStreamService <- MarketDataStreamServiceFs2Grpc.stubResource[IO](managedChannel)
  } yield Resources(supervisor, httpClient, managedChannel, marketDataStreamService)


  def runn(
    marketDataStreamService: MarketDataStreamServiceFs2Grpc[IO, io.grpc.Metadata],
    logger: CustomizedLogger[IO]
  ) = for {
    meta <- AuthMetadata[IO]("t.Qlx4Xgyxa0Tn8XTmHDtcDdov-RqXpoQLWtzqx7FeMG5BuuVz8z_JGAjecjSqGDDGURzcP8gc0EynTS9T4QP1RQ")
    _ <- marketDataStreamService.marketDataServerSideStream(
      MarketDataServerSideStreamRequest(
        subscribeLastPriceRequest = SubscribeLastPriceRequest(
          subscriptionAction = SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE,
          instruments = List(LastPriceInstrument(instrumentId = "BBG0013HGFT4"))
        ).some
      ),
      ctx = meta
    ).flatTap(response => fs2.Stream.eval(println(s"MarketData: ${response.payload.lastPrice}").pure[IO]))
      .compile
      .drain
  } yield ()


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
            configs.rublePulseConfig,
            botModule.bot, logger, graphs
          )
          _ <- resource.supervisor.supervise(rublePulseService.start)
          _ <- botModule.bot.interpret(SendText(ChatIntId(174861972), "App started") :: Nil)
          _ <- runn(resource.marketDataStreamService, logger)
          _ <- logger.info("App started")
          _ <- botModule.bot.start()
        } yield ()
      }
    } yield ()
  }
}
