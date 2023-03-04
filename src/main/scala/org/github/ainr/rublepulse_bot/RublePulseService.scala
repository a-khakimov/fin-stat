package org.github.ainr.rublepulse_bot

import cats.Monad
import cats.data.NonEmptyList
import cats.effect.Concurrent
import cats.syntax.all._
import io.circe.generic.auto._
import io.circe.parser._
import org.github.ainr.configurations.RublePulseConfig
import org.github.ainr.graphs.{Graphs, Input}
import org.github.ainr.kafka.Consumer
import org.github.ainr.logger.CustomizedLogger
import org.github.ainr.telegram.reaction.{BotReactionsInterpreter, SendPhoto}
import org.nspl
import telegramium.bots.InputPartFile

import java.io.File

trait RublePulseService[F[_]] {
  def start: F[Unit]
}

object RublePulseService {
  def apply[F[_]: Monad: Concurrent](
    config: RublePulseConfig,
    consumer: Consumer[F],
    bot: BotReactionsInterpreter[F],
    logger: CustomizedLogger[F],
    graphs: Graphs[F]
  ): RublePulseService[F] = new RublePulseService[F] {

    private def processEvents(chunk: List[(String, String)]): F[Unit] = {
      val lastPrices =
        chunk
          .map { case (_, value) => decode[LastPriceEvent](value) }
          .collect { case Right(event) if event.figi === config.figi => event }
          .map(_.price)

      println(s"Last prices ${lastPrices.mkString(", ")}")

      lastPrices
        .toNel
        .map(movingAverage(_, 20))
        .flatMap(_.toNel)
        .flatMap(prices => analyse(prices).map(result => (prices, result)))
        .traverse {
          case (prices, analyseResult) => for {
            charts <- buildCharts(prices.toList)
            _ <- bot.interpret(SendPhoto(chatId = config.chatId, photo = InputPartFile(charts), caption = analyseResult.some) :: Nil)
            _ <- logger.info(s"Processing records: ${lastPrices.mkString(", ")}")
          } yield ()
        }.as(())
      }

    // todo - make it better
    private def analyse(prices: NonEmptyList[Double]): Option[String] = {
      val limit = (prices.head * config.priceLimit) / 100
      val minLimit = prices.head - limit
      val maxLimit = prices.head + limit
      if (prices.last < minLimit) Some(s"Рубль окреп на ${scale(prices.head - prices.last)}")
      else if (prices.last > maxLimit) Some(s"Рубль ослаб на ${scale(prices.last - prices.head)}")
      else None
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

    override def start: F[Unit] = consumer.consume(config.sizeLimit, config.timeLimit, processEvents)
  }
}
