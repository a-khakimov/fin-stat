package org.github.ainr.tinvest

import cats.effect.kernel.Sync
import cats.effect.{Async, IO, IOApp, Resource}
import cats.syntax.all._
import dev.profunktor.redis4cats.connection.RedisClient
import dev.profunktor.redis4cats.effect.Log.Stdout.instance
import fs2.grpc.client.ClientOptions
import io.grpc.ManagedChannel
import org.github.ainr.configurations.Configurations
import org.github.ainr.context.Context
import org.github.ainr.logger.CustomizedLogger
import org.github.ainr.tinvest.lastprice.LastPriceModule
import org.typelevel.log4cats.LoggerName
import org.typelevel.log4cats.slf4j.Slf4jLogger
import ru.tinkoff.piapi.contract.v1.instruments.InstrumentsServiceFs2Grpc
import ru.tinkoff.piapi.contract.v1.marketdata.{MarketDataServiceFs2Grpc, MarketDataStreamServiceFs2Grpc}
import ru.tinkoff.piapi.contract.v1.operations.{OperationsServiceFs2Grpc, OperationsStreamServiceFs2Grpc}

object TInvestApp extends IOApp.Simple {

  private def servicesResource[F[_]: Async](channel: ManagedChannel): Resource[F, Services[F]] = for {
    instrumentsService      <- InstrumentsServiceFs2Grpc.stubResource[F](channel, ClientOptions.default)
    marketDataService       <- MarketDataServiceFs2Grpc.stubResource[F](channel, ClientOptions.default)
    marketDataStreamService <- MarketDataStreamServiceFs2Grpc.stubResource[F](channel, ClientOptions.default)
    operationsStreamService <- OperationsStreamServiceFs2Grpc.stubResource[F](channel, ClientOptions.default)
    operationsService       <- OperationsServiceFs2Grpc.stubResource[F](channel, ClientOptions.default)
  } yield Services(
    instrumentsService,
    marketDataService,
    marketDataStreamService,
    operationsStreamService,
    operationsService
  )

  final case class Resources(
    redisClient: RedisClient,
    grpcChannel: ManagedChannel
  )

  def resources(configs: Configurations): Resource[IO, Resources] = for {
    redisClient <- RedisClient[IO].from("redis://localhost")
    grpcChannel <- Channel[IO](configs.tinkoffInvestApiConfig.url, configs.tinkoffInvestApiConfig.port)
  } yield Resources(redisClient, grpcChannel)

  def run: IO[Unit] = for {
    context <- Context.make
    logger = CustomizedLogger(
      Slf4jLogger.getLogger[IO](Sync[IO], LoggerName("TInvestApp")),
      context
    )
    _ <- Configurations.load[IO](logger).flatMap {
      configs =>
        resources(configs)
          .flatMap(resource => servicesResource[IO](resource.grpcChannel).map(_ -> resource))
          .flatMap {
            case (grpcServices, resource) =>
              for {
                lastPriceModule <- LastPriceModule.build(resource.redisClient, logger, configs, grpcServices)
              } yield lastPriceModule
          }
          .use {
            lastPriceModule => lastPriceModule.run
          }
    }
  } yield ()
}
