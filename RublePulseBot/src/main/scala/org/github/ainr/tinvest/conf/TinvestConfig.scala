package org.github.ainr.tinvest.conf


final case class TinvestConfig(
  url: String,
  port: Int,
  token: String
)

final case class LastPriceSubscriptionsConfig(
  instruments: List[String]
)
