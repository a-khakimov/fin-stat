package org.github.ainr.db

import cats.effect.{Async, Resource}
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.github.ainr.db.conf.PostgresConfig

object Database {

  def makePostgresResource[F[_]: Async](
      config: PostgresConfig
  ): Resource[F, HikariTransactor[F]] = {
    for {
      ec <- ExecutionContexts.fixedThreadPool[F](config.threads)
      xa <- HikariTransactor.newHikariTransactor[F](
        "org.postgresql.Driver",
        config.url,
        config.user,
        config.password,
        ec
      )
    } yield xa
  }
}
