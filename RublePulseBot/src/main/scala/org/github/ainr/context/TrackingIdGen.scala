package org.github.ainr.context

import cats.effect.IO
import cats.effect.std.UUIDGen
import org.github.ainr.logger.LogKeys

trait TrackingIdGen[F[_]] {

  def gen(): F[Unit]
}

object TrackingIdGen {

  def apply(context: Context[IO]): TrackingIdGen[IO] = new TrackingIdGen[IO] {

    override def gen(): IO[Unit] = for {
      trackingID <- UUIDGen.randomString[IO]
      _ <- context.set(LogKeys.trackingID, trackingID.take(8).toUpperCase)
    } yield ()
  }
}
