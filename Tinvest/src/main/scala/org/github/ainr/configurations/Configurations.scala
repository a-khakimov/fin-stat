package org.github.ainr.configurations

import cats.Show
import cats.effect.Async
import cats.syntax.all._
import ciris.{ConfigValue, Effect, Secret}
import com.typesafe.config.ConfigFactory
import lt.dvim.ciris.Hocon._
import org.github.ainr.logger.CustomizedLogger
import show._

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
  token: Secret[String]
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

object show {
  implicit val showProducerConfig = new Show[ProducerConfig] {
    override def show(t: ProducerConfig): String = s"ProducerConfig[url=${t.url} topic=${t.topic}]"
  }

  implicit val showConfigurations = new Show[Configurations] {
    override def show(t: Configurations): String =
      s"""Configurations:
        |tinkoffInvestApiConfig=${t.rublePulseConfig}
        |tinvestConfig=${t.tinvestConfig}
        |consumer=${t.consumer}
        |rublePulseConfig=${t.rublePulseConfig}
        |""".stripMargin
  }
}

object Configurations {

  def load[F[_]: Async](logger: CustomizedLogger[F]): F[Configurations] = {

    val config = ConfigFactory.load("reference.conf")

    val tinkoffInvestApi = hoconAt(config)("tinkoff_invest_api")
    val tinvest: HoconAt = hoconAt(config)("tinvest")
    val consumer = hoconAt(config)("consumer")
    val rublePulse = hoconAt(config)("rublePulse")

    val tinkoffInvestApiConfig: ConfigValue[Effect, TinkoffInvestApiConfig] = (
      tinkoffInvestApi("url").as[String],
      tinkoffInvestApi("port").as[Int],
      tinkoffInvestApi("token").as[String].secret
    ).mapN(TinkoffInvestApiConfig.apply)

    def producerConfig(path: String): ConfigValue[Effect, ProducerConfig] = (
      tinvest(s"$path.url").as[String],
      tinvest(s"$path.topic").as[String]
    ).mapN(ProducerConfig.apply)

    val consumerConfig: ConfigValue[Effect, ConsumerConfig] = (
      consumer("url").as[String],
      consumer("topic").as[String],
      consumer("groupId").as[String]
    ).mapN(ConsumerConfig.apply)

    val tinvestConfig = (
      tinvest("subscribes.lastPricesFor").as[List[String]].map(Subscribes),
      tinvest("portfolio.accounts").as[List[String]].map(Portfolio),
      (
        producerConfig("producers.lastPriceEvents"),
        producerConfig("producers.portfolioEvents")
      ).mapN(Producers),
    ).mapN(TinvestConfig)

    val rublePulseConfig: ConfigValue[Effect, RublePulseConfig] = (
      rublePulse("figi").as[String],
      rublePulse("priceLimit").as[Float],
      rublePulse("timeLimit").as[FiniteDuration],
      rublePulse("sizeLimit").as[Int]
    ).mapN(RublePulseConfig.apply)

    val configsF = (
      tinkoffInvestApiConfig, tinvestConfig, consumerConfig, rublePulseConfig
    ).mapN(Configurations.apply).load[F]

    configsF <* configsF.map(configs => println(configs.show))
  }
}