package org.github.ainr.tinvest.lastprice


import cats.syntax.all._
import cats.effect.{Async, Concurrent}
import org.github.ainr.configurations.Configurations
import org.github.ainr.kafka.Producer
import org.github.ainr.logger.CustomizedLogger
import org.github.ainr.tinvest.Services

object LastPriceModule {
  def run[F[_]: Async: Concurrent](
    logger: CustomizedLogger[F],
    configs: Configurations,
    services: Services[F],
  ): F[Unit] = for {
    producer <- Producer(configs.tinvestConfig.producers.lastPriceEvents, logger)
    lastPriceRepository <- LastPriceRepository(configs, services.marketDataStreamService, logger)
    lastPriceService = new LastPriceService(configs.tinvestConfig, lastPriceRepository, producer)
    _ <- lastPriceService.stream.flatMap(_.compile.drain)
  } yield ()
}
