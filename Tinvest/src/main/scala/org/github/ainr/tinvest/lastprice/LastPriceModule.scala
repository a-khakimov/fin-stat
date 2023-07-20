package org.github.ainr.tinvest.lastprice


import cats.effect.{Async, Concurrent, Resource}
import cats.syntax.all._
import fs2.concurrent.Topic
import org.github.ainr.configurations.Configurations
import org.github.ainr.logger.CustomizedLogger
import org.github.ainr.tinvest.Services


trait LastPriceModule[F[_]] {
  def run: F[Unit]
}

object LastPriceModule {
  def build[F[_]: Async: Concurrent](
    logger: CustomizedLogger[F],
    configs: Configurations,
    services: Services[F],
    topic: Topic[F, LastPriceEvent]
  ): Resource[F, LastPriceModule[F]] = for {
    lastPriceRepository <- Resource.eval(LastPriceRepository(configs, services.marketDataStreamService, logger))
    lastPriceService = new LastPriceService(configs.tinvestConfig, lastPriceRepository, topic)
  } yield new LastPriceModule[F] {
    override def run: F[Unit] = lastPriceService.stream.flatMap(_.compile.drain)
  }
}
