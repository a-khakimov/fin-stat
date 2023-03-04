package org.github.ainr.tinvest.portfolio

import cats.Monad
import cats.effect.Temporal
import cats.syntax.all._
import fs2.kafka.ProducerResult
import org.github.ainr.configurations.TinvestConfig
import org.github.ainr.kafka.Producer
import org.github.ainr.logger.CustomizedLogger
import ru.tinkoff.piapi.contract.v1.common.{MoneyValue => GMoneyValue}
import ru.tinkoff.piapi.contract.v1.operations.PortfolioResponse

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class PortfolioService[F[_]: Monad: Temporal](
  configs: TinvestConfig,
  portfolioRepository: PortfolioRepository[F],
  logger: CustomizedLogger[F],
  producer: Producer[F]
) {

  def stream: fs2.Stream[F, ProducerResult[Unit, String, String]] = {
    producer.produce {
      fs2.Stream
        .awakeEvery(10 second) // todo - make it configurable
        .evalMap(_ => configs.portfolio.accounts.traverse(portfolioRepository.getPortfolio))
        .map(toPortfolioEvent)
        .map("portfolio" -> _.toString)
    }
  }

  private def toPortfolioEvent(
    portfolio: List[PortfolioResponse]
  ): PortfolioEvent = {
    val toMoneyValue: GMoneyValue => MoneyValue = g => MoneyValue(g.currency, g.units, g.nano)
    PortfolioEvent(portfolio.map(p => Account(p.accountId, p.totalAmountPortfolio.map(toMoneyValue))))
  }
}
