package org.github.ainr.tinvest

import cats.effect.kernel.Sync
import cats.effect.{IO, Resource}
import cats.syntax.all._
import fs2.grpc.client.ClientOptions
import org.github.ainr.configurations.Configurations
import org.github.ainr.context.Context
import org.github.ainr.logger.CustomizedLogger
import org.github.ainr.tinvest.lastprice.LastPriceModule
import org.github.ainr.tinvest.portfolio.PortfolioModule
import org.typelevel.log4cats.LoggerName
import org.typelevel.log4cats.slf4j.Slf4jLogger
import ru.tinkoff.piapi.contract.v1.instruments.InstrumentsServiceFs2Grpc
import ru.tinkoff.piapi.contract.v1.marketdata.{MarketDataServiceFs2Grpc, MarketDataStreamServiceFs2Grpc}
import ru.tinkoff.piapi.contract.v1.operations.{OperationsServiceFs2Grpc, OperationsStreamServiceFs2Grpc}

object TInvestApp {

  private def servicesResource(configs: Configurations): Resource[IO, Services[IO]] = for {
    channel <- Resource.eval(Channel[IO](configs.tinkoffInvestApiConfig.url, configs.tinkoffInvestApiConfig.port))
    instrumentsService <- InstrumentsServiceFs2Grpc.stubResource[IO](channel, ClientOptions.default)
    marketDataService <- MarketDataServiceFs2Grpc.stubResource[IO](channel, ClientOptions.default)
    marketDataStreamService <- MarketDataStreamServiceFs2Grpc.stubResource[IO](channel, ClientOptions.default)
    operationsStreamService <- OperationsStreamServiceFs2Grpc.stubResource[IO](channel, ClientOptions.default)
    operationsService <- OperationsServiceFs2Grpc.stubResource[IO](channel, ClientOptions.default)
  } yield Services(
    instrumentsService,
    marketDataService,
    marketDataStreamService,
    operationsStreamService,
    operationsService
  )

  def run: IO[Unit] = {
    Configurations.load[IO].flatMap {
      configs => servicesResource(configs).use {
        grpcServices => for {
          context <- Context.make
          logger = CustomizedLogger(
            Slf4jLogger.getLogger[IO](Sync[IO], LoggerName("TInvestApp")),
            context
          )
          _ <- List(
            LastPriceModule.run(logger, configs, grpcServices),
            PortfolioModule.run(logger, configs, grpcServices)
          ).parSequence
        } yield ()
      }
    }
  }
}
