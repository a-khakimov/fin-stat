package org.github.ainr.telegram

import cats.effect.IO
import org.github.ainr.context.{Context, TrackingIdGen}
import org.github.ainr.logger.CustomizedLogger
import org.github.ainr.telegram.conf.TelegramConfig
import org.github.ainr.telegram.handler.Handler
import org.github.ainr.telegram.handler.handlers.{Start, Version}
import org.github.ainr.telegram.reaction.BotReactionsInterpreter
import org.http4s.client.Client
import telegramium.bots.high.{Api, BotApi, LongPollBot => TgLongPollBot}

trait BotModule {
  def bot: TgLongPollBot[IO] with BotReactionsInterpreter[IO]
}

object BotModule {

  def apply(
      config: TelegramConfig,
      httpClient: Client[IO],
  )(
      context: Context[IO],
      logger: CustomizedLogger[IO],
      trackingIdGen: TrackingIdGen[IO]
  ): BotModule = new BotModule {

    private val botApi: Api[IO] = BotApi(
      http = httpClient,
      baseUrl = s"${config.url}/bot${config.token}"
    )

    val versionHandler: Version[IO] = Version[IO]()
    val startHandler: Start[IO] = Start[IO]()

    val handlers: Handler[IO] = Handler[IO](
      versionHandler,
      startHandler
    )(logger)

    override val bot: TgLongPollBot[IO] with BotReactionsInterpreter[IO] =
      LongPollBot.make(botApi, handlers)(context, logger, trackingIdGen)
  }
}
