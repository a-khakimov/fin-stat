package org.github.ainr.tinvest

import cats.syntax.all._
import cats.effect.{IO, Resource}
import fs2.grpc.client.ClientOptions
import org.github.ainr.tinvest.configurations.Configurations
import ru.tinkoff.piapi.contract.v1.instruments.InstrumentsServiceFs2Grpc
import ru.tinkoff.piapi.contract.v1.marketdata.{LastPriceInstrument, MarketDataServerSideStreamRequest, MarketDataServiceFs2Grpc, MarketDataStreamServiceFs2Grpc, SubscribeLastPriceRequest, SubscriptionAction}

object TInvestApp {

  private def servicesResource: Resource[IO, Services[IO]] = for {
    channel <- Resource.eval(Channel[IO]("invest-public-api.tinkoff.ru", 443))
    instrumentsServiceFs2Grpc <- InstrumentsServiceFs2Grpc.stubResource[IO](channel, ClientOptions.default)
    marketDataService <- MarketDataServiceFs2Grpc.stubResource[IO](channel, ClientOptions.default)
    marketDataStreamService <- MarketDataStreamServiceFs2Grpc.stubResource[IO](channel, ClientOptions.default)
  } yield Services(instrumentsServiceFs2Grpc, marketDataService, marketDataStreamService)

  def run(): IO[Unit] = {
    servicesResource.use {
      services => for {
        configurations <- Configurations.load[IO]
        _ <- IO.println(configurations)
        metadata <- AuthMetadata[IO](configurations.tinvest.token)
        _ <- {
          val baseStream =
            services.marketDataStreamService
              .marketDataServerSideStream(
                MarketDataServerSideStreamRequest(
                  subscribeLastPriceRequest = SubscribeLastPriceRequest(
                    subscriptionAction = SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE,
                    instruments = LastPriceInstrument(figi = "BBG006L8G4H1") :: Nil
                  ).some
                ),
                metadata
              )
              .flatTap {
                resp => fs2.Stream.eval(IO.println(resp.payload.lastPrice.flatMap(_.price)))
              }
          baseStream
            .handleErrorWith { error => fs2.Stream.eval(IO.println(s"FAIL2: $error")) ++ baseStream }
            .compile
            .drain
        }
      } yield ()
    }
  }
}
