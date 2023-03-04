package org.github.ainr.telegram.handler

import cats.syntax.all._
import cats.{Applicative, Monad}
import cats.effect.Clock
import org.github.ainr.telegram.handler.handlers.{Start, Version}
import org.github.ainr.telegram.reaction.Reaction
import org.github.ainr.logger.CustomizedLogger
import telegramium.bots.{CallbackQuery, Message}

trait Handler[F[_]] {
  def onMessage(message: Message): F[List[Reaction]]

  def onCallbackQuery(query: CallbackQuery): F[List[Reaction]]
}

object Handler {

  def apply[F[_]: Monad: Clock](
      version: Version[F],
      start: Start[F]
  )(
      logger: CustomizedLogger[F]
  ): Handler[F] = new Handler[F] {

    override def onMessage(message: Message): F[List[Reaction]] = for {
      _ <- logger.info(s"Message - ${message.text.getOrElse("Empty")}")
      startTime <- Clock[F].realTime
      reactions <- message.text match {
        case Some("/start")         => start.handle(message)
        case Some("/version")       => version.handle(message)
        case _                      => Applicative[F].pure(Nil)
      }
      endTime <- Clock[F].realTime
      _ <- logger.info(s"Request processing time is ${endTime - startTime}")
    } yield reactions

    override def onCallbackQuery(query: CallbackQuery): F[List[Reaction]] = for {
      _ <- logger.info(s"Message - ${query.message.getOrElse("Empty")}")
    } yield Nil
  }
}
