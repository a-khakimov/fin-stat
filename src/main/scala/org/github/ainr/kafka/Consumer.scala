package org.github.ainr.kafka

import cats.effect.Async
import cats.syntax.all._
import fs2.kafka.{AutoOffsetReset, ConsumerSettings, KafkaConsumer}
import org.github.ainr.configurations.ConsumerConfig
import org.github.ainr.logger.CustomizedLogger

import scala.concurrent.duration.FiniteDuration

trait Consumer[F[_]] {
  def consume(processEvent: (String, String) => F[Unit]): F[Unit]

  def consume(chunkSize: Int, timeout: FiniteDuration, processEvents: List[(String, String)] => F[Unit]): F[Unit]
}

object Consumer {

  def apply[F[_]: Async](
    config: ConsumerConfig,
    logger: CustomizedLogger[F]
  ): F[Consumer[F]] = {

    val consumerSettings: ConsumerSettings[F, String, String] =
      ConsumerSettings[F, String, String]
        .withAutoOffsetReset(AutoOffsetReset.Latest)
        .withAllowAutoCreateTopics(false)
        .withBootstrapServers(config.url)
        .withGroupId(config.groupId)

    Async[F].delay {
      new Consumer[F] {

        val consumerStream: fs2.Stream[F, KafkaConsumer[F, String, String]] =
          KafkaConsumer.stream(consumerSettings).subscribeTo(config.topic)

        def handleError(fallback: fs2.Stream[F, Unit])(cause: Throwable): fs2.Stream[F, Unit] =
          fs2.Stream.eval(logger.error(cause)(s"Consumer error")) *> fallback

        override def consume(processEvent: (String, String) => F[Unit]): F[Unit] = {

          val consumer: fs2.Stream[F, Unit] =
            consumerStream
              .records
              .evalMap {
                committable =>
                  processEvent(committable.record.key, committable.record.value)
              }

          for {
            _ <- logger.info(s"Consumer subscribed to ${config.topic}")
            fallbackToDefault = consumer.handleErrorWith(handleError(consumer))
            stream = fallbackToDefault.handleErrorWith(handleError(fallbackToDefault))
            _ <- logger.info("Consumer started")
            _ <- stream.compile.drain
          } yield ()
        }

        override def consume(
          chunkSize: Int,
          timeout: FiniteDuration,
          processEvents: List[(String, String)] => F[Unit]
        ): F[Unit] = {

          val consumer: fs2.Stream[F, Unit] =
            consumerStream
              .records
              .groupWithin(chunkSize, timeout)
              .evalMap {
                chunk => processEvents(
                  chunk.toList.map(committable => (committable.record.key, committable.record.value))
                )
              }

          for {
            _ <- logger.info(s"Consumer subscribed to ${config.topic}")
            fallbackToDefault = consumer.handleErrorWith(handleError(consumer))
            stream = fallbackToDefault.handleErrorWith(handleError(fallbackToDefault))
            _ <- logger.info("Consumer started")
            _ <- stream.compile.drain
          } yield ()
        }
      }
    }
  }
}
