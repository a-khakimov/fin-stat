package org.github.ainr.tinvest.lastprice

import cats.effect.Async
import cats.syntax.all._
import io.grpc.Metadata
import org.github.ainr.configurations.Configurations
import org.github.ainr.logger.CustomizedLogger
import org.github.ainr.tinvest.AuthMetadata
import ru.tinkoff.piapi.contract.v1.marketdata.{MarketDataResponse, MarketDataServerSideStreamRequest, MarketDataStreamServiceFs2Grpc}

trait LastPriceRepository[F[_]] {
  def stream(request: MarketDataServerSideStreamRequest): F[fs2.Stream[F, MarketDataResponse]]
}

object LastPriceRepository {
  def apply[F[_]: Async](
    configurations: Configurations,
    marketDataStreamService: MarketDataStreamServiceFs2Grpc[F, Metadata],
    logger: CustomizedLogger[F]
  ): F[LastPriceRepository[F]] = Async[F].delay {

    new LastPriceRepository[F] {
      override def stream(request: MarketDataServerSideStreamRequest): F[fs2.Stream[F, MarketDataResponse]] = {
        for {
          metadata <- AuthMetadata[F](configurations.tinkoffInvestApiConfig.token)
          baseStream = marketDataStreamService.marketDataServerSideStream(request, metadata)
        } yield baseStream.handleErrorWith {
          cause =>
            fs2.Stream
              .eval(logger.error(cause)(s"Market data server side stream failed"))
              .flatMap(_ => baseStream)
        }
      }
    }
  }
}
