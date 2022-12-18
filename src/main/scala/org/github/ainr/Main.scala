package org.github.ainr

import cats.effect.{IO, IOApp}
import cats.syntax.all._
import fs2.kafka._
import org.github.ainr.tinvest.TInvestApp

import scala.concurrent.duration._
import scala.language.postfixOps

object Main extends IOApp.Simple {

  override def run: IO[Unit] = {
    List(
      TInvestApp.run()
    ).parSequence.void
  }

  private val consumerSettings: ConsumerSettings[IO, String, String] =
    ConsumerSettings[IO, String, String]
      .withAutoOffsetReset(AutoOffsetReset.Latest)
      .withBootstrapServers("localhost:9092")
      .withGroupId("group")

  private val producerSettings: ProducerSettings[IO, String, String] =
    ProducerSettings[IO, String, String]
      .withBootstrapServers("localhost:9092")

  def processRecord(
    record: ConsumerRecord[String, String]
  ): IO[(String, String)] =
    IO.pure(record.key -> record.value)

  val consumerStream =
    KafkaConsumer
      .stream(consumerSettings)
      .subscribeTo("quickstart-topic")
      .records
      .flatMap { record =>
        fs2.Stream.eval(IO.println(s"< $record")).as(record)
      }
      .groupWithin(10000, 20 second)
      .flatMap { chunk =>
        fs2.Stream.eval(IO.println(s"< CHUNK[${chunk.size}](${chunk})")).as(())
      }
      .recoverWith { error =>
        fs2.Stream.eval(IO.println(s"< $error")).as(())
      }

  val producerStream = KafkaProducer
    .stream(producerSettings)
    .flatMap { producer =>
      fs2
        .Stream(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        .flatMap(i => fs2.Stream.sleep[IO](1000 milliseconds) *> fs2.Stream(i))
        .repeat
        .map { c =>
          val record = ProducerRecord[String, String](
            "quickstart-topic",
            "key",
            c.toString
          )
          ProducerRecords.one(record)
        }
        .evalMap { record =>
          producer.produce(record).flatten <* IO.println(s"> $record")
        }
    }

  val converter: fs2.Stream[IO, Unit] = {
    fs2
      .Stream(0 to 3)
      .flatMap(i => fs2.Stream.sleep[IO](1 milliseconds) *> fs2.Stream(i))
      .repeat
      .groupWithin(10000, 20 second)
      .map { c =>
        println(c.size)
      }
  }

  //def run: IO[Unit] = (consumerStream concurrently producerStream).compile.drain
  //converter.compile.drain
}
