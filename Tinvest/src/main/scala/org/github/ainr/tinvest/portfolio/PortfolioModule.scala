package org.github.ainr.tinvest.portfolio

import cats.effect.{Async, Concurrent}
import cats.syntax.all._
import fs2.concurrent.Topic
import org.github.ainr.configurations.Configurations
import org.github.ainr.logger.CustomizedLogger
import org.github.ainr.tinvest.Services

object PortfolioModule {

  def run[F[_] : Async : Concurrent](
    logger: CustomizedLogger[F],
    configs: Configurations,
    services: Services[F],
    topic: Topic[F, PortfolioEvent]
  ): F[Unit] = for {
    portfolioRepository <- PortfolioRepository(
      logger, configs,
      services.operationsStreamService,
      services.operationsService
    )
    portfolioService = new PortfolioService(configs.tinvestConfig, portfolioRepository, logger, topic)
    _ <- portfolioService.stream.compile.drain
  } yield ()
}
