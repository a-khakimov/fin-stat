package org.github.ainr.logger

trait LoggerContext {

  implicit class LoggerContextSyntax(ctx: Map[String, String]) {

    def withSource(
        name: sourcecode.FullName,
        line: sourcecode.Line
    ): Map[String, String] = {
      ctx.updated(LogKeys.source, s"${name.value}.${line.value}")
    }

    def withTrackingId(context: Map[String, String]): Map[String, String] = {
      context
        .get(LogKeys.trackingID)
        .map(trackingID => ctx.updated(LogKeys.trackingID, trackingID))
        .getOrElse(ctx)
    }

    def withChatId(context: Map[String, String]): Map[String, String] = {
      context
        .get(LogKeys.chatID)
        .map(chatID => ctx.updated(LogKeys.chatID, chatID))
        .getOrElse(ctx)
    }
  }
}
