package org.github.ainr.tinvest.lastprice

import cats.Monad
import cats.effect.kernel.Temporal
import cats.syntax.all._
import fs2.concurrent.Topic
import org.github.ainr.configurations.TinvestConfig
import ru.tinkoff.piapi.contract.v1.marketdata._

class LastPriceService[F[_]: Monad: Temporal](
  configs: TinvestConfig,
  lastPriceRepository: LastPriceRepository[F],
  topic: Topic[F, LastPriceEvent]
) {

  def stream: F[fs2.Stream[F, Unit]] = {
    lastPriceRepository
      .stream(
        MarketDataServerSideStreamRequest(
          subscribeLastPriceRequest = SubscribeLastPriceRequest(
            subscriptionAction = SubscriptionAction.SUBSCRIPTION_ACTION_SUBSCRIBE,
            instruments = configs.subscribes.lastPricesFor.map(figi => LastPriceInstrument(figi = figi))
          ).some
        )
      )
      .map { marketData =>
        marketData
          .map(getLastPrice)
          .collect { case Some(a) => a }
          .map(topic.publish1)
      }
  }

  def getLastPrice(response: MarketDataResponse): Option[LastPriceEvent] = { // todo - replace raw String to Type
    (
      response.payload.lastPrice.map(_.figi),
      response.payload.lastPrice.flatMap(_.price.flatMap(quotationToDouble))
    ).mapN(LastPriceEvent)
  }

  private val quotationToDouble: ru.tinkoff.piapi.contract.v1.common.Quotation => Option[Double] =
    quotation => s"${quotation.units}.${quotation.nano}".toDoubleOption
}
