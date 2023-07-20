package org.github.ainr.tinvest

import cats.effect.{Async, Resource}
import fs2.grpc.client.ClientOptions
import io.grpc.{ManagedChannel, Metadata}
import ru.tinkoff.piapi.contract.v1.instruments.InstrumentsServiceFs2Grpc
import ru.tinkoff.piapi.contract.v1.marketdata.{MarketDataServiceFs2Grpc, MarketDataStreamServiceFs2Grpc}
import ru.tinkoff.piapi.contract.v1.operations.{OperationsServiceFs2Grpc, OperationsStreamServiceFs2Grpc}

final case class Services[F[_]](
  instrumentsService: InstrumentsServiceFs2Grpc[F, Metadata],
  marketDataService: MarketDataServiceFs2Grpc[F, Metadata],
  marketDataStreamService: MarketDataStreamServiceFs2Grpc[F, Metadata],
  operationsStreamService: OperationsStreamServiceFs2Grpc[F, Metadata],
  operationsService: OperationsServiceFs2Grpc[F, Metadata]
)

object Services {

  def build[F[_]: Async](channel: ManagedChannel): Resource[F, Services[F]] = for {
    instrumentsService <- InstrumentsServiceFs2Grpc.stubResource[F](channel, ClientOptions.default)
    marketDataService <- MarketDataServiceFs2Grpc.stubResource[F](channel, ClientOptions.default)
    marketDataStreamService <- MarketDataStreamServiceFs2Grpc.stubResource[F](channel, ClientOptions.default)
    operationsStreamService <- OperationsStreamServiceFs2Grpc.stubResource[F](channel, ClientOptions.default)
    operationsService <- OperationsServiceFs2Grpc.stubResource[F](channel, ClientOptions.default)
  } yield Services(
    instrumentsService,
    marketDataService,
    marketDataStreamService,
    operationsStreamService,
    operationsService
  )
}
