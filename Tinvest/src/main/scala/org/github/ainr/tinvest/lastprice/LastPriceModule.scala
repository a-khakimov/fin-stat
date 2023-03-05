package org.github.ainr.tinvest.lastprice


import cats.effect.{Async, Concurrent, Resource}
import cats.syntax.all._
import dev.profunktor.redis4cats.connection.RedisClient
import dev.profunktor.redis4cats.data.RedisChannel
import dev.profunktor.redis4cats.effect.Log.Stdout.instance
import org.github.ainr.configurations.Configurations
import org.github.ainr.logger.CustomizedLogger
import org.github.ainr.redis.producer.RedisProducer
import org.github.ainr.tinvest.Services


trait LastPriceModule[F[_]] {
  def run: F[Unit]
}

object LastPriceModule {
  def build[F[_]: Async: Concurrent](
    redisClient: RedisClient,
    logger: CustomizedLogger[F],
    configs: Configurations,
    services: Services[F],
  ): Resource[F, LastPriceModule[F]] = for {
    producer <- RedisProducer[F, LastPriceEvent](redisClient, RedisChannel("last_price_events")) // configs.tinvestConfig.producers.lastPriceEvents
    lastPriceRepository <- Resource.eval(LastPriceRepository(configs, services.marketDataStreamService, logger))
    lastPriceService = new LastPriceService(configs.tinvestConfig, lastPriceRepository, producer)
  } yield new LastPriceModule[F] {
    override def run: F[Unit] = lastPriceService.stream.flatMap(_.compile.drain)
  }
}
