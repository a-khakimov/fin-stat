package org.github.ainr

import cats.effect.{IO, IOApp}
import org.github.ainr.tinvest.TInvestApp

object Main extends IOApp.Simple {
  override def run: IO[Unit] = TInvestApp.run
}