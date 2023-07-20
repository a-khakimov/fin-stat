package org.github.ainr.tinvest

import cats.effect.kernel.Sync
import cats.effect.{IO, IOApp, Resource}
import fs2.concurrent.Topic
import io.grpc.ManagedChannel
import org.github.ainr.configurations.Configurations
import org.github.ainr.context.Context
import org.github.ainr.logger.CustomizedLogger
import org.github.ainr.tinvest.lastprice.{LastPriceEvent, LastPriceModule}
import org.typelevel.log4cats.LoggerName
import org.typelevel.log4cats.slf4j.Slf4jLogger

object TInvestApp extends IOApp.Simple {

  private final case class Resources(
    grpcChannel: ManagedChannel
  )

  private def resources(configs: Configurations): Resource[IO, Resources] = for {
    grpcChannel <- Channel[IO](configs.tinkoffInvestApiConfig.url, configs.tinkoffInvestApiConfig.port)
  } yield Resources(grpcChannel)

  def run: IO[Unit] = for {
    context <- Context.make
    logger = CustomizedLogger(
      Slf4jLogger.getLogger[IO](Sync[IO], LoggerName("TInvestApp")),
      context
    )
    _ <- Configurations.load[IO](logger).flatMap {
      configs =>
        resources(configs)
          .flatMap(resource => Services.build[IO](resource.grpcChannel).map(_ -> resource))
          .flatMap {
            case (grpcServices, resource) =>
              for {
                topic <- Resource.eval(Topic[IO, LastPriceEvent])
                lastPriceModule <- LastPriceModule.build(logger, configs, grpcServices, topic)
              } yield lastPriceModule
          }
          .use {
            lastPriceModule => lastPriceModule.run
          }
    }
  } yield ()
}
