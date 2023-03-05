package org.github.ainr

import cats.effect.{IO, IOApp}
import org.github.ainr.rublepulse.RublePulseApp

object Main extends IOApp.Simple {
  override def run: IO[Unit] = RublePulseApp.run
}
