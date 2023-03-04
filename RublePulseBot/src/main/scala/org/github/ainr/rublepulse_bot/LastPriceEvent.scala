package org.github.ainr.rublepulse_bot

final case class LastPriceEvent(
  figi: String,
  price: Double
)
