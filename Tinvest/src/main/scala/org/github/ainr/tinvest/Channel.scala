package org.github.ainr.tinvest

import cats.effect.{Resource, Sync}
import io.grpc.ManagedChannel
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder

object Channel {
  def apply[F[_]: Sync](host: String, port: Int): Resource[F, ManagedChannel] = Resource.eval {
    Sync[F].delay {
      NettyChannelBuilder
        .forAddress(host, port)
        .keepAliveWithoutCalls(true)
        .build()
    }
  }
}
