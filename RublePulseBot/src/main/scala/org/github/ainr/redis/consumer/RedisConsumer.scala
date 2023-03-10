package org.github.ainr.redis.consumer

import cats.effect.{Async, Resource}
import cats.implicits.catsSyntaxApplicativeId
import dev.profunktor.redis4cats.connection.RedisClient
import dev.profunktor.redis4cats.data.{RedisChannel, RedisCodec}
import dev.profunktor.redis4cats.effect.Log
import dev.profunktor.redis4cats.pubsub.PubSub
import fs2.{Pipe, Stream}

object RedisConsumer {
  def apply[F[_]: Async: Log, V](
    client: RedisClient,
    channel: RedisChannel[String]
  ): Resource[F, Consumer[F, V]] = {

    PubSub
      .mkSubscriberConnection[F, String, String](client, RedisCodec.Utf8)
      .map { subscriber =>
        new Consumer[F, V] {

          def print: Pipe[F, String, String] = _.evalTap(s => println(s).pure)

          override def consume(decode: Pipe[F, String, V]): Stream[F, V] = {
            subscriber
              .subscribe(channel)
              .through(print)
              .through(decode)
          }
        }
      }
  }
}
