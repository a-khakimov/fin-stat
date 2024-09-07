package org.github.ainr.db.repo

import cats.syntax.all._
import cats.effect.{Async, Resource}
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import org.github.ainr.logger.CustomizedLogger
import ru.tinkoff.piapi.contract.v1.marketdata.LastPrice

trait TinvestRepository[F[_]] {

  def saveInstrumentLastPrice(lastPrice: LastPrice): F[Unit]
}

object TinvestRepository {

  def make[F[_]: Async](
    transactor: Transactor[F],
    logger: CustomizedLogger[F]
  ): Resource[F, TinvestRepository[F]] = Resource.eval {
    Async[F].delay {
      new TinvestRepository[F] {

        import com.google.protobuf.timestamp.Timestamp
        import java.time.Instant

        def toInstant(timestamp: Timestamp): Instant = {
          Instant.ofEpochSecond(timestamp.seconds, timestamp.nanos)
        }


        override def saveInstrumentLastPrice(lastPrice: LastPrice): F[Unit] = {
          sql"""
               |INSERT INTO last_prices (instrumentUid, figi, app_time, tinvest_time, units, nano)
               |VALUES (
               |  ${lastPrice.instrumentUid},
               |  ${lastPrice.figi},
               |  NOW(),
               |  ${lastPrice.time.map(toInstant)},
               |  ${lastPrice.price.map(_.units)},
               |  ${lastPrice.price.map(_.nano)}
               |)
               |""".stripMargin
            .update
            .run
            .transact(transactor)
            .flatMap(num => logger.info(s"Last price saved ($num): ${lastPrice.instrumentUid}"))
            .recoverWith {
              case cause => logger.error(cause)(s"Last price saving error")
            }
        }
      }
    }
  }
}
