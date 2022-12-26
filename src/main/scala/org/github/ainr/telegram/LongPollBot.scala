package org.github.ainr.telegram

import cats.effect.IO
import cats.syntax.all._
import org.github.ainr.context.{Context, TrackingIdGen}
import org.github.ainr.logger.{CustomizedLogger, LogKeys}
import org.github.ainr.telegram.handler.Handler
import org.github.ainr.telegram.reaction._
import telegramium.bots.high.implicits.methodOps
import telegramium.bots.high.{Api, Methods, LongPollBot => TelegramiumLongPollBot}
import telegramium.bots._

object LongPollBot {

  def make(
      api: Api[IO],
      handler: Handler[IO]
  )(
      context: Context[IO],
      logger: CustomizedLogger[IO],
      trackingId: TrackingIdGen[IO]
  ): TelegramiumLongPollBot[IO] with BotReactionsInterpreter[IO] = {

    new TelegramiumLongPollBot[IO](api) with BotReactionsInterpreter[IO] {

      override def onMessage(msg: Message): IO[Unit] = {
        for {
          _ <- trackingId.gen()
          _ <- context.set(LogKeys.chatID, msg.chat.id.toString)
          reactions <- handler.onMessage(msg).recoverWith {
            case cause => logger.error(cause)("Something went wrong").as(Nil)
          }
          _ <- interpret(reactions).recoverWith {
            case cause => logger.error(cause)("Something went wrong")
          }
        } yield ()
      }

      override def onEditedMessage(msg: Message): IO[Unit] =
        logger.info(s"onEditedMessage - $msg")

      override def onChannelPost(msg: Message): IO[Unit] =
        logger.info(s"onChannelPost - $msg")

      override def onEditedChannelPost(msg: Message): IO[Unit] =
        logger.info(s"EditedChannelPost - $msg")

      override def onInlineQuery(query: InlineQuery): IO[Unit] =
        logger.info(s"InlineQuery - $query")

      override def onCallbackQuery(query: CallbackQuery): IO[Unit] = {
        for {
          _ <- trackingId.gen()
          _ <- context.set(LogKeys.chatID, query.from.id.toString)
          reactions <- handler.onCallbackQuery(query).recoverWith {
            case cause => logger.error(cause)("Something went wrong").as(Nil)
          }
          _ <- interpret(reactions).recoverWith {
            case cause => logger.error(cause)("Something went wrong")
          }
        } yield ()
      }

      override def onChosenInlineResult(inlineResult: ChosenInlineResult): IO[Unit] =
        logger.info(s"ChosenInlineResult - $inlineResult")

      override def onShippingQuery(query: ShippingQuery): IO[Unit] =
        logger.info(s"ShippingQuery - $query")

      override def onPreCheckoutQuery(query: PreCheckoutQuery): IO[Unit] =
        logger.info(s"PreCheckoutQuery - $query")

      override def onPoll(poll: Poll): IO[Unit] =
        logger.info(s"Poll - $poll")

      override def onPollAnswer(pollAnswer: PollAnswer): IO[Unit] =
        logger.info(s"PollAnswer - $pollAnswer")

      override def onMyChatMember(myChatMember: ChatMemberUpdated): IO[Unit] =
        logger.info(s"MyChatMember - $myChatMember")

      override def onChatMember(chatMember: ChatMemberUpdated): IO[Unit] =
        logger.info(s"ChatMember - $chatMember")

      override def onChatJoinRequest(request: ChatJoinRequest): IO[Unit] =
        logger.info(s"ChatJoinRequest - $request")

      override def interpret(reactions: List[Reaction]): IO[Unit] = {
        reactions.foldLeft(IO.unit) {
          case (prevF, reaction) => prevF.flatMap {
              _ =>
                reaction match {
                  case reaction: SendText     => text(reaction)
                  case reaction: EditMessage  => editMessage(reaction)
                  case reaction: SendDocument => document(reaction)
                  case reaction: SendPhoto    => photo(reaction)
                  case reaction: Sleep        => IO.sleep(reaction.delay)
                }
            }
        }
      }

      private def text(reaction: SendText): IO[Unit] = {
        Methods
          .sendMessage(
            chatId = reaction.chatId,
            text = reaction.text,
            parseMode = reaction.parseMode,
            replyMarkup = reaction.replyMarkup
          )
          .exec(api)
          .void
      }

      private def editMessage(reaction: EditMessage): IO[Unit] = {
        Methods
          .editMessageText(
            chatId = reaction.chatId,
            messageId = reaction.messageId,
            inlineMessageId = reaction.inlineMessageId,
            text = reaction.text,
            parseMode = reaction.parseMode,
            entities = reaction.entities,
            disableWebPagePreview = reaction.disableWebPagePreview,
            replyMarkup = reaction.replyMarkup
          )
          .exec(api)
          .void
      }

      private def document(reaction: SendDocument): IO[Unit] = {
        Methods
          .sendDocument(
            chatId = reaction.chatId,
            document = reaction.document,
            caption = reaction.caption,
            parseMode = reaction.parseMode,
            replyMarkup = reaction.replyMarkup
          )
          .exec(api)
          .void
      }

      private def photo(reaction: SendPhoto): IO[Unit] = {
        Methods
          .sendPhoto(
            chatId = reaction.chatId,
            photo = reaction.photo,
            caption = reaction.caption,
            parseMode = reaction.parseMode,
            replyMarkup = reaction.replyMarkup
          )
          .exec(api)
          .void
      }
    }
  }
}
