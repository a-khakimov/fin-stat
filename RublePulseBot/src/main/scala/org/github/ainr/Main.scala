package org.github.ainr

import cats.effect.{IO, IOApp}
import org.github.ainr.rublepulse_bot.RublePulseApp

object Main extends IOApp.Simple {
  override def run: IO[Unit] = RublePulseApp.run
}
