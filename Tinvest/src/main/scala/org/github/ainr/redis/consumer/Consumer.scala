package org.github.ainr.redis.consumer

import fs2.{Pipe, Stream}

trait Consumer[F[_], V] {
  def consume(decode: Pipe[F, String, V]): Stream[F, V]
}
