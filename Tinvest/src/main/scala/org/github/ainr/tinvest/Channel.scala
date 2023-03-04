package org.github.ainr.tinvest

import cats.effect.Sync
import io.grpc.ManagedChannel
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder

object Channel {
  def apply[F[_]: Sync](host: String, port: Int): F[ManagedChannel] = Sync[F].delay {
    NettyChannelBuilder
      .forAddress(host, port)
      .keepAliveWithoutCalls(true)
      .build()
  }
}
