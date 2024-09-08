package org.github.ainr.rublepulse

import cats.Monad
import cats.data.NonEmptyList
import cats.effect.{Concurrent, Temporal}
import cats.implicits.catsSyntaxList
import cats.syntax.all._
import org.github.ainr.configurations.RublePulseConfig
import org.github.ainr.graphs.{Graphs, Input}
import org.github.ainr.logger.CustomizedLogger
import org.github.ainr.telegram.reaction.{BotReactionsInterpreter, SendPhoto}
import org.nspl
import telegramium.bots.InputPartFile

import java.io.File

trait RublePulseService[F[_]] {
  def process(lastPrices: List[Double]): F[Unit]
}

object RublePulseService {
  def apply[F[_]: Monad: Concurrent: Temporal: CustomizedLogger](
    config: RublePulseConfig,
    bot: BotReactionsInterpreter[F],
    graphs: Graphs[F]
  ): RublePulseService[F] = new RublePulseService[F] {

    def process(lastPrices: List[Double]): F[Unit] = {
      lastPrices
        .toNel
        .map(movingAverage(_, Math.min(lastPrices.size / 2, 20)))
        .mproduct(analyse)
        .traverse_ {
          case (prices, analyseResult) => for {
            charts <- buildCharts(prices, lastPrices)
            _ <- bot.interpret(SendPhoto(chatId = config.chatId, photo = InputPartFile(charts), caption = analyseResult.some))
            _ <- CustomizedLogger[F].info(s"Processing records: ${lastPrices.mkString(", ")}")
            _ <- CustomizedLogger[F].info(s"Processing records: ${prices.toList.mkString(", ")}")
          } yield ()
        }
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

    private def movingAverage(values: NonEmptyList[Double], period: Int): NonEmptyList[Double] = {
      val first = values.take(period).sum / period
      val subtract = values.toList.map(_ / period)
      val add = subtract.drop(period)
      val addAndSubtract = add.zip(subtract).map(Function.tupled(_ - _))
      addAndSubtract.scanLeftNel(first)(_ + _)
    }

    private def scale(double: Double): Double =
      BigDecimal(double)
        .setScale(2, BigDecimal.RoundingMode.HALF_DOWN)
        .toDouble

    private def buildCharts(prices: NonEmptyList[Double], prices2: List[Double]): F[File] = graphs.plot(
      Input(
        List(
          Input.Data(
            seq = prices.toList,
            main = s"USD/RUB",
            xlab = {config.figi},
            ylab = s"RUB",
            color = nspl.RedBlue(0, prices.maximum)
          ),
          Input.Data(
            seq = prices2,
            main = s"USD/RUB 2",
            xlab = {config.figi},
            ylab = s"RUB",
            color = nspl.RedBlue(0, 0.2)
          ),
        )
      )
    )
  }
}
