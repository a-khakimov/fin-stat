package org.github.ainr.redis.producer

import cats.effect.{Async, Resource, Sync}
import cats.implicits.catsSyntaxApplicativeId
import dev.profunktor.redis4cats.connection.RedisClient
import dev.profunktor.redis4cats.data.{RedisChannel, RedisCodec}
import dev.profunktor.redis4cats.effect.Log
import dev.profunktor.redis4cats.pubsub.PubSub
import fs2.{Pipe, Stream}


object RedisProducer {
  def apply[F[_]: Async: Log, V](
    client: RedisClient,
    channel: RedisChannel[String]
  ): Resource[F, Producer[F, V]] = for {
    publisher <- PubSub.mkPublisherConnection[F, String, String](client, RedisCodec.Utf8)
    producer <- Resource.eval {
      Sync[F].delay {
        new Producer[F, V] {

          def print: Pipe[F, String, String] = _.evalTap(s => println(s).pure)

          override def produce(
            events: Stream[F, V]
          )(encode: Pipe[F, V, String]): Stream[F, Unit] = {
            publisher.publish(channel)(events.through(encode).through(print))
          }
        }
      }
    }
  } yield producer
}
