package org.github.ainr.context

import cats.effect.{IO, IOLocal}

trait Context[F[_]] {

  def set(key: String, value: String): F[Unit]

  def getAll: F[Map[String, String]]

  def get(key: String): F[Unit]
}

object Context {

  def make: IO[Context[IO]] = {
    IOLocal(Map.empty[String, String]).map {
      local =>
        new Context[IO] {
          override def set(key: String, value: String): IO[Unit] = for {
            current <- local.get.map(context => context.updated(key, value))
            _ <- local.set(current.updated(key, value))
          } yield ()

          override def getAll: IO[Map[String, String]] = local.get

          override def get(key: String): IO[Unit] = local.get.map(_.get(key))
        }
    }
  }
}
