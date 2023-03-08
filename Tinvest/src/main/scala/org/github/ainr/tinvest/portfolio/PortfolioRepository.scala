package org.github.ainr.tinvest.portfolio

import cats.effect.Async
import cats.syntax.all._
import io.grpc.Metadata
import org.github.ainr.configurations.Configurations
import org.github.ainr.logger.CustomizedLogger
import org.github.ainr.tinvest.AuthMetadata
import ru.tinkoff.piapi.contract.v1.operations.{OperationsServiceFs2Grpc, OperationsStreamServiceFs2Grpc, PortfolioRequest, PortfolioResponse, PortfolioStreamRequest, PortfolioStreamResponse}

trait PortfolioRepository[F[_]] {

  def portfolioStream(accounts: List[String]): F[fs2.Stream[F, PortfolioStreamResponse]]

  def getPortfolio(account: String): F[PortfolioResponse]
}

object PortfolioRepository {
  def apply[F[_]: Async](
    logger: CustomizedLogger[F],
    configs: Configurations,
    operationsStreamService: OperationsStreamServiceFs2Grpc[F, Metadata],
    operationsService: OperationsServiceFs2Grpc[F, Metadata],
  ): F[PortfolioRepository[F]] = Async[F].delay {

    new PortfolioRepository[F] {

      override def portfolioStream(accounts: List[String]): F[fs2.Stream[F, PortfolioStreamResponse]] = {
        for {
          metadata <- authMetadata
          baseStream = operationsStreamService.portfolioStream(PortfolioStreamRequest(accounts), metadata)
        } yield baseStream.handleErrorWith {
          cause =>
            fs2.Stream
              .eval(logger.error(cause)(s"Portfolio stream failed"))
              .flatMap(_ => baseStream)
        }
      }

      def getPortfolio(account: String): F[PortfolioResponse] = for {
        metadata <- authMetadata
        portfolio <- operationsService.getPortfolio(PortfolioRequest(account), metadata)
      } yield portfolio

      private def authMetadata: F[Metadata] = AuthMetadata[F](configs.tinkoffInvestApiConfig.token.value)
    }
  }
}
