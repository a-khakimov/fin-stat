package org.github.ainr.redis.producer

import cats.effect.{Async, Resource}
import dev.profunktor.redis4cats.connection.RedisClient
import dev.profunktor.redis4cats.data.{RedisChannel, RedisCodec}
import dev.profunktor.redis4cats.effect.Log
import dev.profunktor.redis4cats.pubsub.PubSub
import fs2.{Pipe, Stream}


object RedisProducer {
  def apply[F[_]: Async: Log, V](
    client: RedisClient,
    channel: RedisChannel[String]
  ): Resource[F, Producer[F, V]] = {
    PubSub
      .mkPublisherConnection[F, String, String](client, RedisCodec.Utf8)
      .map { publisher =>
        new Producer[F, V] {
          override def produce(
            v: Stream[F, V]
          )(encode: Pipe[F, V, String]): Stream[F, Unit] = {
            publisher.publish(channel)(v.through(encode))
          }
        }
      }
  }
}
