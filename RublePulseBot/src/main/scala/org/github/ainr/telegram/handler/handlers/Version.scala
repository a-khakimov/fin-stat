package org.github.ainr.telegram.handler.handlers

import cats.Applicative
import cats.syntax.all._
import org.github.ainr.BuildInfo
import org.github.ainr.telegram.reaction.{Reaction, SendText}
import telegramium.bots.{ChatIntId, InlineKeyboardButton, InlineKeyboardMarkup, Markdown2, Message}

import java.text.SimpleDateFormat

trait Version[F[_]] {
  def handle(message: Message): F[List[Reaction]]
}

object Version {
  def apply[F[_]: Applicative](): Version[F] = new Version[F] {
    override def handle(message: Message): F[List[Reaction]] = {
      List(
        SendText(
          chatId = ChatIntId(message.chat.id),
          parseMode = Markdown2.some,
          text =
            s"""```
               |${BuildInfo.name} ${BuildInfo.version}
               |Commit: ${BuildInfo.gitHeadCommit.getOrElse("-")}
               |Build time: ${dateTimeFormat.format(BuildInfo.buildTime)}
               |```""".stripMargin,
          replyMarkup = InlineKeyboardMarkup(
            (InlineKeyboardButton("Github", BuildInfo.github.some) :: Nil) :: Nil
          ).some
        )
      ).widen[Reaction].pure[F]
    }

    val dateTimeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
  }
}
