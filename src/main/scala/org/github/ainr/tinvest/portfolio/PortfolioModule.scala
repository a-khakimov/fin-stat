package org.github.ainr.tinvest.portfolio

import cats.effect.{Async, Concurrent}
import cats.syntax.all._
import org.github.ainr.configurations.Configurations
import org.github.ainr.kafka.Producer
import org.github.ainr.logger.CustomizedLogger
import org.github.ainr.tinvest.Services

object PortfolioModule {

  def run[F[_] : Async : Concurrent](
    logger: CustomizedLogger[F],
    configs: Configurations,
    services: Services[F],
  ): F[Unit] = for {
    producer <- Producer(configs.tinvestConfig.producers.portfolioEvents, logger)
    portfolioRepository <- PortfolioRepository(
      logger, configs,
      services.operationsStreamService,
      services.operationsService
    )
    portfolioService = new PortfolioService(configs.tinvestConfig, portfolioRepository, logger, producer)
    _ <- portfolioService.stream.compile.drain
  } yield ()
}
