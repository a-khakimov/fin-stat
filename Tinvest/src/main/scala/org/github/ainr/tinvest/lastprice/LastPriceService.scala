package org.github.ainr.tinvest.lastprice

import cats.Monad
import cats.effect.kernel.Temporal
import cats.syntax.all._
import io.circe.generic.auto._
import io.circe.syntax._
import org.github.ainr.configurations.TinvestConfig
import org.github.ainr.redis.producer.Producer
import ru.tinkoff.piapi.contract.v1.marketdata._

import scala.concurrent.duration.DurationInt

class LastPriceService[F[_]: Monad: Temporal](
  configs: TinvestConfig,
  lastPriceRepository: LastPriceRepository[F],
  producer: Producer[F, LastPriceEvent]
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
        producer.produce(
          //fs2.Stream.awakeEvery(1000 milliseconds) >> fs2.Stream.eval(LastPriceEvent("1212", 1).pure)
          marketData
            .map(getLastPrice)
            .collect { case Some(a) => a }
        )(toJson)
      }
  }

  private def toJson: fs2.Pipe[F, LastPriceEvent, String] =
    _.evalMap(_.asJson.noSpaces.pure[F])

  def getLastPrice(response: MarketDataResponse): Option[LastPriceEvent] = { // todo - replace raw String to Type
    (
      response.payload.lastPrice.map(_.figi),
      response.payload.lastPrice.flatMap(_.price.flatMap(quotationToDouble))
    ).mapN(LastPriceEvent)
  }

  private val quotationToDouble: ru.tinkoff.piapi.contract.v1.common.Quotation => Option[Double] =
    quotation => s"${quotation.units}.${quotation.nano}".toDoubleOption
}
