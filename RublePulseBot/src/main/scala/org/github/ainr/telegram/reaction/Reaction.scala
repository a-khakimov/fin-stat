package org.github.ainr.telegram.reaction

import telegramium.bots.{ChatId, ChatIntId, IFile, InlineKeyboardMarkup, KeyboardMarkup, MessageEntity, ParseMode}

import scala.concurrent.duration.FiniteDuration

trait Reaction

final case class Sleep(
    delay: FiniteDuration
) extends Reaction

final case class SendText(
    chatId: ChatIntId,
    text: String,
    parseMode: Option[ParseMode] = None,
    replyMarkup: Option[KeyboardMarkup] = Option.empty
) extends Reaction

final case class EditMessage(
    chatId: Option[ChatId] = Option.empty,
    messageId: Option[Int] = Option.empty,
    inlineMessageId: Option[String] = Option.empty,
    text: String,
    parseMode: Option[ParseMode] = Option.empty,
    entities: List[MessageEntity] = List.empty,
    replyMarkup: Option[InlineKeyboardMarkup] = Option.empty
) extends Reaction

final case class SendDocument(
    chatId: ChatIntId,
    document: IFile,
    caption: Option[String] = Option.empty,
    parseMode: Option[ParseMode] = Option.empty,
    replyMarkup: Option[KeyboardMarkup] = Option.empty
) extends Reaction

final case class SendPhoto(
    chatId: ChatIntId,
    photo: IFile,
    caption: Option[String] = Option.empty,
    parseMode: Option[ParseMode] = Option.empty,
    replyMarkup: Option[KeyboardMarkup] = Option.empty
) extends Reaction
