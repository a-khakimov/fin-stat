package org.github.ainr.configurations

import cats.effect.Async
import cats.syntax.all._
import ciris.{ConfigValue, Effect}
import com.typesafe.config.ConfigFactory
import lt.dvim.ciris.Hocon._
import org.github.ainr.db.conf.PostgresConfig
import org.github.ainr.telegram.conf.TelegramConfig
import org.github.ainr.tinvest.conf.TinvestConfig
import telegramium.bots.ChatIntId

import scala.concurrent.duration.FiniteDuration

final case class Configurations(
  tinvestConfig: TinvestConfig,
  telegram: TelegramConfig,
  postgres: PostgresConfig,
  rublePulseConfig: RublePulseConfig
)


final case class RublePulseConfig(
  figi: String,
  chatId: ChatIntId,
  priceLimit: Float, // percents
  timeLimit: FiniteDuration,
  sizeLimit: Int,
)

object Configurations {

  def load[F[_]: Async]: F[Configurations] = {

    val config = ConfigFactory.load("reference.conf")

    val tinvest = hoconAt(config)("tinvest")
    val telegram = hoconAt(config)("telegram")
    val postgres = hoconAt(config)("postgres")
    val rublePulse = hoconAt(config)("rublePulse")

    val telegramConfig: ConfigValue[Effect, TelegramConfig] = (
      telegram("url").as[String],
      telegram("token").as[String]
    ).mapN(TelegramConfig.apply)

    val tinvestConfig: ConfigValue[Effect, TinvestConfig] = (
      tinvest("url").as[String],
      tinvest("port").as[Int],
      tinvest("token").as[String],
    ).mapN(TinvestConfig.apply)


    val postgresConfig: ConfigValue[Effect, PostgresConfig] = (
      postgres("threads").as[Int],
      postgres("url").as[String],
      postgres("user").as[String],
      postgres("password").as[String],
    ).mapN(PostgresConfig.apply)

    val rublePulseConfig: ConfigValue[Effect, RublePulseConfig] = (
      rublePulse("figi").as[String],
      rublePulse("chatId").as[Long].map(ChatIntId),
      rublePulse("priceLimit").as[Float],
      rublePulse("timeLimit").as[FiniteDuration],
      rublePulse("sizeLimit").as[Int]
    ).mapN(RublePulseConfig.apply)

    (tinvestConfig, telegramConfig, postgresConfig, rublePulseConfig)
      .mapN(Configurations.apply)
      .load[F]
  }
}