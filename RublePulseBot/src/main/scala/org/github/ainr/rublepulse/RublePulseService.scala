package org.github.ainr.rublepulse

import cats.Monad
import cats.data.NonEmptyList
import cats.effect.{Concurrent, Temporal}
import cats.syntax.all._
import fs2.Chunk
import io.circe.generic.auto._
import io.circe.parser._
import org.github.ainr.configurations.RublePulseConfig
import org.github.ainr.graphs.{Graphs, Input}
import org.github.ainr.logger.CustomizedLogger
import org.github.ainr.redis.consumer.Consumer
import org.github.ainr.telegram.reaction.{BotReactionsInterpreter, SendPhoto}
import org.nspl
import telegramium.bots.InputPartFile

import java.io.File

trait RublePulseService[F[_]] {
  def start: F[Unit]
}

object RublePulseService {
  def apply[F[_]: Monad: Concurrent: Temporal](
    config: RublePulseConfig,
    consumer: Consumer[F, LastPriceEvent],
    bot: BotReactionsInterpreter[F],
    logger: CustomizedLogger[F],
    graphs: Graphs[F]
  ): RublePulseService[F] = new RublePulseService[F] {

    private def processEvents(lastPrices: Chunk[LastPriceEvent]): F[Unit] = {
      println(s"Last prices ${lastPrices.toList.mkString(", ")}")

      lastPrices
        .toNel
        .map(_.map(_.price))
        .map(movingAverage(_, 20))
        .flatMap(_.toNel)
        .flatMap(prices => analyse(prices).map(result => (prices, result)))
        .traverse {
          case (prices, analyseResult) => for {
            charts <- buildCharts(prices.toList)
            _ <- bot.interpret(SendPhoto(chatId = config.chatId, photo = InputPartFile(charts), caption = analyseResult.some) :: Nil)
            _ <- logger.info(s"Processing records: ${lastPrices.toList.mkString(", ")}")
          } yield ()
        }.as(())
      }

    // todo - make it better
    private def analyse(prices: NonEmptyList[Double]): Option[String] = {
      val limit = (prices.head * config.priceLimit) / 100
      val minLimit = prices.head - limit
      val maxLimit = prices.head + limit
      prices.last match {
        case price if price < minLimit => Some(s"Рубль окреп на ${scale(prices.head - prices.last)}")
        case price if price > maxLimit => Some(s"Рубль ослаб на ${scale(prices.last - prices.head)}")
        case _ => None
      }
    }

    private def movingAverage(values: NonEmptyList[Double], period: Int): List[Double] = {
      val first = values.take(period).sum / period
      val subtract = values.toList.map(_ / period)
      val add = subtract.drop(period)
      val addAndSubtract = add.zip(subtract).map(Function.tupled(_ - _))
      addAndSubtract.foldLeft(first :: List.fill(period - 1)(values.head)) {
        (acc, add) => (add + acc.head) :: acc
      }.reverse
    }

    private def scale(double: Double): Double =
      BigDecimal(double)
        .setScale(2, BigDecimal.RoundingMode.HALF_DOWN)
        .toDouble

    private def buildCharts(prices: List[Double]): F[File] = graphs.plot(
      Input(
        List(
          Input.Data(
            seq = prices,
            main = s"USD/RUB",
            xlab = {config.figi},
            ylab = s"RUB",
            color = nspl.RedBlue(0, prices.maxOption.getOrElse(200 /* :holypeka: */))
          )
        )
      )
    )

    // TODO: Create repo layer
    private def fromJson: fs2.Pipe[F, String, LastPriceEvent] =
      _.evalMap(e => decode[LastPriceEvent](e).pure[F])
        .collect {
          case Right(event) if event.figi === config.figi => event
        }

    override def start: F[Unit] =
      consumer
        .consume(fromJson)
        .groupWithin(config.sizeLimit, config.timeLimit)
        .evalMap(processEvents)
        .compile
        .drain
  }
}
