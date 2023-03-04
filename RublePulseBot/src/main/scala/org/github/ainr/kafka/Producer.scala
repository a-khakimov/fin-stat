package org.github.ainr.kafka

import cats.effect.Async
import cats.syntax.all._
import fs2.kafka._
import org.github.ainr.configurations.ProducerConfig
import org.github.ainr.logger.CustomizedLogger

import scala.language.postfixOps

trait Producer[F[_]] {
  def produce(stream: fs2.Stream[F, (String, String)]): fs2.Stream[F, ProducerResult[Unit, String, String]]
}

object Producer {

  def apply[F[_]: Async](
    config: ProducerConfig,
    logger: CustomizedLogger[F]
  ): F[Producer[F]] = Async[F].delay {

    new Producer[F] {
      override def produce(
        stream: fs2.Stream[F, (String, String)]
      ): fs2.Stream[F, ProducerResult[Unit, String, String]] =
        KafkaProducer
          .stream(ProducerSettings[F, String, String].withBootstrapServers(config.url)) // todo resource
          .flatMap {
            producer =>
              stream
                .map(toRecord)
                .evalMap { records =>
                  producer.produce(records).flatten <*
                    logger.info(s"Producer records - ${records.records}")
                }
        }

      def toRecord(data: (String, String)): ProducerRecords[Unit, String, String] = {
        val (key, value) = data
        ProducerRecords.one(
          ProducerRecord[String, String](
            config.topic, key, value
          )
        )
      }
    }
  }
}
