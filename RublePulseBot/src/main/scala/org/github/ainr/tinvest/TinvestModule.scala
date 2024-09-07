package org.github.ainr.tinvest

import cats.syntax.all._
import cats.effect.Async
import cats.effect.Resource
import cats.effect.std.Supervisor
import fs2.grpc.syntax.all._
import io.grpc.Metadata
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import org.github.ainr.logger.CustomizedLogger
import org.github.ainr.tinvest.conf.TinvestConfig
import ru.tinkoff.piapi.contract.v1.instruments.{FindInstrumentRequest, InstrumentsServiceFs2Grpc}
import ru.tinkoff.piapi.contract.v1.marketdata._


final case class TinvestModule[F[_]](
  marketDataStreamService: MarketDataStreamServiceFs2Grpc[F, Metadata],
  instrumentsService: InstrumentsServiceFs2Grpc[F, Metadata]
)

object TinvestModule {
  def make[F[_]: Async](
    conf: TinvestConfig,
    supervisor: Supervisor[F],
    logger: CustomizedLogger[F]
  ): Resource[F, TinvestModule[F]] = for {
    managedChannel <- NettyChannelBuilder.forAddress(conf.url, conf.port).resource[F]
    meta <- Resource.eval(AuthMetadata[F](conf.token))
    marketDataStreamService <- MarketDataStreamServiceFs2Grpc.stubResource[F](managedChannel)
    instrumentsService <- InstrumentsServiceFs2Grpc.stubResource[F](managedChannel)
    findInstrumentResponse <- Resource.eval {
      instrumentsService.findInstrument(FindInstrumentRequest(
        query = "BBG0013HGFT4"
      ), meta)
    }
    _ <- Resource.eval(
      findInstrumentResponse.instruments.traverse(
        inst =>
          logger.info(s"Intr ${inst.name}, ${inst.instrumentType}, ${inst.ticker}, ${inst.isin}")
      )
    )
    _ <- Resource.eval {
      supervisor.supervise {
        marketDataStreamService.marketDataServerSideStream(
            MarketDataServerSideStreamRequest(
              subscribeLastPriceRequest = SubscribeLastPriceRequest(
                subscriptionAction = SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE,
                instruments = List(LastPriceInstrument(instrumentId = "BBG0013HGFT4"))
              ).some
            ),
            ctx = meta
          )
          .flatTap(response => fs2.Stream.eval(logger.info(s"MarketData: ${response}").pure[F]))
          .compile
          .drain
      }
    }
  } yield TinvestModule(marketDataStreamService, instrumentsService)
}