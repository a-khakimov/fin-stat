package org.github.ainr.redis.producer

import fs2.{Pipe, Stream}

trait Producer[F[_], V] {
  def produce(v: Stream[F, V])(encode: Pipe[F, V, String]): Stream[F, Unit]
}
