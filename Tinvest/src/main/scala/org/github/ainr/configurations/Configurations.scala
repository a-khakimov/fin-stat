package org.github.ainr.configurations

import cats.effect.Async
import cats.syntax.all._
import ciris.{ConfigValue, Effect}
import com.typesafe.config.ConfigFactory
import lt.dvim.ciris.Hocon._

import scala.concurrent.duration.FiniteDuration

final case class Configurations(
  tinkoffInvestApiConfig: TinkoffInvestApiConfig,
  tinvestConfig: TinvestConfig,
  consumer: ConsumerConfig,
  rublePulseConfig: RublePulseConfig
)

final case class TinkoffInvestApiConfig(
  url: String,
  port: Int,
  token: String
)

final case class ProducerConfig(
  url: String,
  topic: String,
)

final case class ConsumerConfig(
  url: String,
  topic: String,
  groupId: String
)

final case class RublePulseConfig(
  figi: String,
  priceLimit: Float, // percents
  timeLimit: FiniteDuration,
  sizeLimit: Int,
)

final case class TinvestConfig(
  subscribes: Subscribes,
  portfolio: Portfolio,
  producers: Producers
)

final case class Producers(
  lastPriceEvents: ProducerConfig,
  portfolioEvents: ProducerConfig
)

final case class Subscribes(
  lastPricesFor: List[String]
)

final case class Portfolio(
  accounts: List[String]
)

object Configurations {

  def load[F[_]: Async]: F[Configurations] = {

    val config = ConfigFactory.load("reference.conf")

    val tinkoffInvestApi = hoconAt(config)("tinkoff_invest_api")
    val tinvest: HoconAt = hoconAt(config)("tinvest")
    val consumer = hoconAt(config)("consumer")
    val rublePulse = hoconAt(config)("rublePulse")

    val tinkoffInvestApiConfig: ConfigValue[Effect, TinkoffInvestApiConfig] = (
      tinkoffInvestApi("url").as[String],
      tinkoffInvestApi("port").as[Int],
      tinkoffInvestApi("token").as[String]
    ).mapN(TinkoffInvestApiConfig.apply)

    def producerConfig(hocon: HoconAt, path: String): ConfigValue[Effect, ProducerConfig] = (
      hocon(s"$path.url").as[String],
      hocon(s"$path.topic").as[String]
    ).mapN(ProducerConfig.apply)

    val consumerConfig: ConfigValue[Effect, ConsumerConfig] = (
      consumer("url").as[String],
      consumer("topic").as[String],
      consumer("groupId").as[String]
    ).mapN(ConsumerConfig.apply)

    val tinvestConfig = {
      (
        producerConfig(tinvest, "producers.lastPriceEvents"),
        producerConfig(tinvest, "producers.portfolioEvents")
      ).flatMapN { case (lastPriceProducer, portfolioProducer) =>
        ConfigValue.default(
          TinvestConfig(
            Subscribes(toList(config.getStringList("tinvest.subscribes.lastPricesFor"))), // WTF? ciris hasn't List[T] decoder
            Portfolio(toList(config.getStringList("tinvest.portfolio.accounts"))),
            Producers(lastPriceProducer, portfolioProducer)
          )
        )
      }
    }

    val rublePulseConfig: ConfigValue[Effect, RublePulseConfig] = (
      rublePulse("figi").as[String],
      rublePulse("priceLimit").as[Float],
      rublePulse("timeLimit").as[FiniteDuration],
      rublePulse("sizeLimit").as[Int]
    ).mapN(RublePulseConfig.apply)

    (tinkoffInvestApiConfig, tinvestConfig, consumerConfig, rublePulseConfig)
      .mapN(Configurations.apply)
      .load[F]
  }

  // this is not ok
  def toList[A](collection: java.util.Collection[A]): List[A] = {
    val builder = List.newBuilder[A]
    val it = collection.iterator()
    while (it.hasNext) builder += it.next()
    builder.result()
  }
}