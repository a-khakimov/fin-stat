package org.github.ainr.kafka

final case class Event[T](key: String, value: T)
