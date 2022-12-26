package org.github.ainr

import cats.effect.{IO, IOApp}
import cats.syntax.all._
import org.github.ainr.rublepulse_bot.RublePulseApp
import org.github.ainr.tinvest.TInvestApp

import scala.language.postfixOps

object Main extends IOApp.Simple {

  override def run: IO[Unit] = {
    List(
      TInvestApp.run,
      RublePulseApp.run
    ).parSequence.void
  }
}
