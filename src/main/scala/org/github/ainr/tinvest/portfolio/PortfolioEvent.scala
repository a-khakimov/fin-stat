package org.github.ainr.tinvest.portfolio

import io.circe.syntax.EncoderOps
import io.circe.generic.auto._

final case class PortfolioEvent(
  accounts: List[Account]
) { self =>
  override def toString: String = {
    self.asJson.noSpacesSortKeys
  }
}

final case class Account(
  accountId: String,
  totalAmount: Option[MoneyValue]
)

final case class MoneyValue(
  currency: String,
  units: Long,
  nano: Int,
)