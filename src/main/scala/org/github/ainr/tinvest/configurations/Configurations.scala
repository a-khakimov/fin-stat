package org.github.ainr.tinvest.configurations

import cats.effect.Async
import cats.syntax.all._
import ciris.{ConfigValue, Effect}
import com.typesafe.config.ConfigFactory
import lt.dvim.ciris.Hocon._

final case class Configurations(
  tinvest: TinvestConfig,
)

final case class TinvestConfig(
  url: String,
  port: Int,
  token: String
)

object Configurations {

  def load[F[_]: Async]: F[Configurations] = {

    val config = ConfigFactory.load("reference.conf")

    val tinvest = hoconAt(config)("tinvest")

    val tinvestConfig: ConfigValue[Effect, TinvestConfig] = (
      tinvest("url").as[String],
      tinvest("port").as[Int],
      tinvest("token").as[String]
    ).mapN(TinvestConfig.apply)

    tinvestConfig
      .map(Configurations.apply)
      .load[F]
  }
}