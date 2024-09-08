package org.github.ainr.db.repo

import cats.effect.{Async, Resource}
import cats.syntax.all._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import org.github.ainr.logger.CustomizedLogger
import ru.tinkoff.piapi.contract.v1.marketdata.LastPrice

trait TinvestRepository[F[_]] {

  def saveInstrumentLastPrice(lastPrice: LastPrice): F[Unit]

  def getInstrumentLastPrices(figi: String): F[List[Double]]
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

        private def toInstant(timestamp: Timestamp): Instant =
          Instant.ofEpochSecond(timestamp.seconds, timestamp.nanos)

        private val quotationToDouble: ru.tinkoff.piapi.contract.v1.common.Quotation => Option[Double] =
          quotation => s"${quotation.units}.${quotation.nano}".toDoubleOption

        override def saveInstrumentLastPrice(lastPrice: LastPrice): F[Unit] = {
          sql"""
               |INSERT INTO last_prices (instrumentUid, figi, app_time, tinvest_time, price, processed)
               |VALUES (
               |  ${lastPrice.instrumentUid},
               |  ${lastPrice.figi},
               |  NOW(),
               |  ${lastPrice.time.map(toInstant)},
               |  ${lastPrice.price.flatMap(quotationToDouble)},
               |  false
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
//
        //WHERE figi = $figi AND app_time > NOW() - interval '3 hour'
        override def getInstrumentLastPrices(figi: String): F[List[Double]] = {
          sql"""
               |SELECT app_time, price
               |FROM last_prices
               |ORDER BY app_time;
             """
            .stripMargin
            .query[(Instant, Double)]
            .to[List]
            .transact(transactor)
            .map(c => c.map(_._2))
        }
      }
    }
  }
}
