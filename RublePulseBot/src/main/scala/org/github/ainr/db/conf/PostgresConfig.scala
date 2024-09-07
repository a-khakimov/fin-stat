package org.github.ainr.db.conf

final case class PostgresConfig(
    threads: Int,
    url: String,
    user: String,
    password: String
)
