package org.github.ainr.rublepulse

import cats.syntax.all._
import cats.effect.{Async, Resource, Temporal}
import cats.effect.std.Supervisor
import org.github.ainr.configurations.RublePulseConfig
import org.github.ainr.db.repo.TinvestRepository
import org.github.ainr.graphs.Graphs
import org.github.ainr.logger.CustomizedLogger
import org.github.ainr.telegram.reaction.BotReactionsInterpreter

import java.time.{LocalTime, ZoneId, ZonedDateTime}
import scala.concurrent.duration.{DurationInt, FiniteDuration, SECONDS}

object RublePulseModule {
  import s.StreamsUtil
  def make[F[_]: Async: CustomizedLogger](
    config: RublePulseConfig,
    supervisor: Supervisor[F],
    graphs: Graphs[F],
    bot: BotReactionsInterpreter[F],
    tinvestRepository: TinvestRepository[F]
  ): Resource[F, Unit] = Resource.eval {

    val rublePulseService = RublePulseService(config, bot, graphs)

    supervisor.supervise {
      val targetTime = LocalTime.of(22, 39) // Время для ежедневного запуска (9:00 утра)
      val zoneId = ZoneId.systemDefault() // Часовой пояс
      val unstableStream = fs2.Stream
        .awakeEvery(15 second)
        //.eval(CustomizedLogger[F].info("-----> Run every day stream 1"))
        //.awakeEveryDayAt(targetTime, zoneId)
        .evalMap(_ => CustomizedLogger[F].info("----> Run every day stream 2"))
        .evalMap(_ =>
          tinvestRepository.getInstrumentLastPrices("figi")
            .recoverWith {
              case cause => CustomizedLogger[F].error(cause)("Error").as(Nil)
            }
        )
        .evalTap(lastPrices => CustomizedLogger[F].info(lastPrices.mkString(", ")))
        .evalMap(lastPrices => rublePulseService.process(lastPrices))

      unstableStream
        .onError {
          case cause =>
            CustomizedLogger[F].error(cause)("Stream failed, retrying...")
            fs2.Stream.sleep[F](20 second) >> unstableStream
        }
        .compile
        .drain
    }.map(_ => ())
  }
}

object s {

  // Функция для вычисления времени до следующего запуска в заданное время суток
  private def timeUntilNextRun(targetTime: LocalTime, zoneId: ZoneId): FiniteDuration = {
    val now = ZonedDateTime.now(zoneId)
    val todayTarget = now.toLocalDate.atTime(targetTime).atZone(zoneId)

    val nextRun = if (now.isBefore(todayTarget)) todayTarget else todayTarget.plusDays(1)
    val durationUntilNextRun = java.time.Duration.between(now, nextRun).getSeconds

    FiniteDuration(durationUntilNextRun, SECONDS)
  }

  // Стрим, который будет запускаться каждый день в указанное время
  private def repeat[F[_]: Temporal: CustomizedLogger, O](
    stream: fs2.Stream[F, O],
    targetTime: LocalTime,
    zoneId: ZoneId,
  ): fs2.Stream[F, O] = {
    fs2.Stream.eval(CustomizedLogger[F].info(s"!!!  Start dailyStream targetTime is ${targetTime}, zoneId is ${zoneId}")) >>
      stream >> fs2.Stream.sleep[F](24.hours) >> repeat(stream, targetTime, zoneId)
  }

  implicit class StreamsUtil[+F[_]: Temporal: CustomizedLogger, +O](stream: fs2.Stream[F, O]) {
    def awakeEveryDayAt(targetTime: LocalTime, zoneId: ZoneId): fs2.Stream[F, O] = {
      val initialDelay = timeUntilNextRun(targetTime, zoneId)
      fs2.Stream.sleep[F](initialDelay) >> repeat(stream, targetTime, zoneId)
    }
  }
}