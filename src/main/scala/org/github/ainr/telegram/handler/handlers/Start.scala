package org.github.ainr.telegram.handler.handlers

import cats.Applicative
import cats.syntax.all._
import cats.implicits.catsSyntaxApplicativeId
import org.github.ainr.telegram.reaction.{Reaction, SendText}
import telegramium.bots.{ChatIntId, Message}

trait Start[F[_]] {

  def handle(message: Message): F[List[Reaction]]
}

object Start {

  def apply[F[_]: Applicative](): Start[F] = new Start[F] {

    override def handle(message: Message): F[List[Reaction]] = List(
      SendText(
        ChatIntId(message.chat.id),
        "Hello!"
      )
    ).widen[Reaction].pure[F]
  }
}
