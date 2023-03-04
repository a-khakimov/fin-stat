package org.github.ainr.configurations

import cats.effect.Async
import cats.syntax.all._
import ciris.{ConfigValue, Effect}
import com.typesafe.config.ConfigFactory
import lt.dvim.ciris.Hocon._
import org.github.ainr.telegram.conf.TelegramConfig
import telegramium.bots.ChatIntId

import scala.concurrent.duration.FiniteDuration

final case class Configurations(
  consumer: ConsumerConfig,
  telegram: TelegramConfig,
  rublePulseConfig: RublePulseConfig
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
  chatId: ChatIntId,
  priceLimit: Float, // percents
  timeLimit: FiniteDuration,
  sizeLimit: Int,
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

    val consumer = hoconAt(config)("consumer")
    val telegram = hoconAt(config)("telegram")
    val rublePulse = hoconAt(config)("rublePulse")

    val consumerConfig: ConfigValue[Effect, ConsumerConfig] = (
      consumer("url").as[String],
      consumer("topic").as[String],
      consumer("groupId").as[String]
    ).mapN(ConsumerConfig.apply)

    val telegramConfig: ConfigValue[Effect, TelegramConfig] = (
      telegram("url").as[String],
      telegram("token").as[String]
    ).mapN(TelegramConfig.apply)

    val rublePulseConfig: ConfigValue[Effect, RublePulseConfig] = (
      rublePulse("figi").as[String],
      rublePulse("chatId").as[Long].map(ChatIntId),
      rublePulse("priceLimit").as[Float],
      rublePulse("timeLimit").as[FiniteDuration],
      rublePulse("sizeLimit").as[Int]
    ).mapN(RublePulseConfig.apply)

    (consumerConfig, telegramConfig, rublePulseConfig)
      .mapN(Configurations.apply)
      .load[F]
  }
}