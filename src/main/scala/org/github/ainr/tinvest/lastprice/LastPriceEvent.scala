package org.github.ainr.tinvest.lastprice

import io.circe.syntax.EncoderOps
import io.circe.generic.auto._

final case class LastPriceEvent (
  figi: String,
  price: Double
) { self =>
  override def toString: String = {
    self.asJson.noSpacesSortKeys
  }
}

