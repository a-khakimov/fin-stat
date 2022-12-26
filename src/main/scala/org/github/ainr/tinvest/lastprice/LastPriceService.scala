package org.github.ainr.tinvest.lastprice

import cats.Monad
import cats.syntax.all._
import fs2.kafka.ProducerResult
import org.github.ainr.configurations.TinvestConfig
import org.github.ainr.kafka.Producer
import ru.tinkoff.piapi.contract.v1.marketdata._

class LastPriceService[F[_]: Monad](
  configs: TinvestConfig,
  lastPriceRepository: LastPriceRepository[F],
  producer: Producer[F]
) {

  def stream: F[fs2.Stream[F, ProducerResult[Unit, String, String]]] = {
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
        producer.produce(
          marketData.map(getLastPrice).collect { case Some(a) => a }
        )
      }
  }

  def getLastPrice(response: MarketDataResponse): Option[(String, String)] = { // todo - replace raw String to Type
    val instrumentUid = response.payload.lastPrice.map(_.instrumentUid)
    val event = (
      response.payload.lastPrice.map(_.figi),
      response.payload.lastPrice.flatMap(_.price.flatMap(quotationToDouble))
    ).mapN(LastPriceEvent)

    (instrumentUid, event.map(_.toString)).mapN((_, _))
  }

  private val quotationToDouble: ru.tinkoff.piapi.contract.v1.common.Quotation => Option[Double] =
    quotation => s"${quotation.units}.${quotation.nano}".toDoubleOption
}
