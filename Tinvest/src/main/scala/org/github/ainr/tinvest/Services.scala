package org.github.ainr.tinvest

import io.grpc.Metadata
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
