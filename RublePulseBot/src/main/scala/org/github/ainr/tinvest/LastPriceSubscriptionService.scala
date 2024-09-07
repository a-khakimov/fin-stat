package org.github.ainr.tinvest

import cats.effect.std.Supervisor
import cats.effect.{Async, Resource}
import io.grpc.Metadata
import ru.tinkoff.piapi.contract.v1.marketdata.MarketDataStreamServiceFs2Grpc

trait LastPriceSubscriptionService[F[_]] {
  def subscribe(instrument: String): F[Unit]
}



object LastPriceSubscriptionService {

  def make[F[_]: Async](
    supervisor: Supervisor[F],
    marketDataStreamService: MarketDataStreamServiceFs2Grpc[F, Metadata],
): Resource[F, LastPriceSubscriptionService[F]] = Resource.eval {
    Async[F].delay {
      new LastPriceSubscriptionService[F] {

        override def subscribe(instrument: String): F[Unit] = {
          ???
        }
      }
    }
  }
}
