package org.github.ainr.logger

import cats.Monad
import cats.syntax.all._
import org.github.ainr.context.Context
import org.typelevel.log4cats.SelfAwareStructuredLogger

trait CustomizedLogger[F[_]] {

  def trace(ctx: Map[String, String])(msg: => String)(
      implicit
      name: sourcecode.FullName,
      line: sourcecode.Line
  ): F[Unit]

  def trace(ctx: Map[String, String], t: Throwable)(msg: => String)(
      implicit
      name: sourcecode.FullName,
      line: sourcecode.Line
  ): F[Unit]

  def debug(ctx: Map[String, String])(msg: => String)(
      implicit
      name: sourcecode.FullName,
      line: sourcecode.Line
  ): F[Unit]

  def debug(ctx: Map[String, String], t: Throwable)(msg: => String)(
      implicit
      name: sourcecode.FullName,
      line: sourcecode.Line
  ): F[Unit]

  def info(ctx: Map[String, String])(msg: => String)(
      implicit
      name: sourcecode.FullName,
      line: sourcecode.Line
  ): F[Unit]

  def info(ctx: Map[String, String], t: Throwable)(msg: => String)(
      implicit
      name: sourcecode.FullName,
      line: sourcecode.Line
  ): F[Unit]

  def warn(ctx: Map[String, String])(msg: => String)(
      implicit
      name: sourcecode.FullName,
      line: sourcecode.Line
  ): F[Unit]

  def warn(ctx: Map[String, String], t: Throwable)(msg: => String)(
      implicit
      name: sourcecode.FullName,
      line: sourcecode.Line
  ): F[Unit]

  def error(ctx: Map[String, String])(msg: => String)(
      implicit
      name: sourcecode.FullName,
      line: sourcecode.Line
  ): F[Unit]

  def error(ctx: Map[String, String], t: Throwable)(msg: => String)(
      implicit
      name: sourcecode.FullName,
      line: sourcecode.Line
  ): F[Unit]

  def error(message: => String)(
      implicit
      name: sourcecode.FullName,
      line: sourcecode.Line
  ): F[Unit]

  def warn(message: => String)(
      implicit
      name: sourcecode.FullName,
      line: sourcecode.Line
  ): F[Unit]

  def info(message: => String)(
      implicit
      name: sourcecode.FullName,
      line: sourcecode.Line
  ): F[Unit]

  def debug(message: => String)(
      implicit
      name: sourcecode.FullName,
      line: sourcecode.Line
  ): F[Unit]

  def trace(message: => String)(
      implicit
      name: sourcecode.FullName,
      line: sourcecode.Line
  ): F[Unit]

  def error(t: Throwable)(message: => String)(
      implicit
      name: sourcecode.FullName,
      line: sourcecode.Line
  ): F[Unit]

  def warn(t: Throwable)(message: => String)(
      implicit
      name: sourcecode.FullName,
      line: sourcecode.Line
  ): F[Unit]

  def info(t: Throwable)(message: => String)(
      implicit
      name: sourcecode.FullName,
      line: sourcecode.Line
  ): F[Unit]

  def debug(t: Throwable)(message: => String)(
      implicit
      name: sourcecode.FullName,
      line: sourcecode.Line
  ): F[Unit]

  def trace(t: Throwable)(message: => String)(
      implicit
      name: sourcecode.FullName,
      line: sourcecode.Line
  ): F[Unit]
}

object CustomizedLogger {

  def apply[F[_]](implicit logger: CustomizedLogger[F]): CustomizedLogger[F] = implicitly

  def make[F[_]: Monad](
      logger: SelfAwareStructuredLogger[F],
      context: Context[F]
  ): CustomizedLogger[F] = {

    val emptyLogContext = Map.empty[String, String]

    new SelfAwareStructuredLogger[F] with CustomizedLogger[F] with LoggerContext {

      override def isTraceEnabled: F[Boolean] = logger.isTraceEnabled

      override def isDebugEnabled: F[Boolean] = logger.isDebugEnabled

      override def isInfoEnabled: F[Boolean] = logger.isInfoEnabled

      override def isWarnEnabled: F[Boolean] = logger.isWarnEnabled

      override def isErrorEnabled: F[Boolean] = logger.isErrorEnabled

      override def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
        logger.trace(ctx)(msg)

      override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        logger.trace(ctx, t)(msg)

      override def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
        logger.debug(ctx)(msg)

      override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        logger.debug(ctx, t)(msg)

      override def info(ctx: Map[String, String])(msg: => String): F[Unit] =
        logger.info(ctx)(msg)

      override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        logger.info(ctx, t)(msg)

      override def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
        logger.warn(ctx)(msg)

      override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        logger.warn(ctx, t)(msg)

      override def error(ctx: Map[String, String])(msg: => String): F[Unit] =
        logger.error(ctx)(msg)

      override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        logger.error(ctx, t)(msg)

      override def error(message: => String): F[Unit] =
        logger.error(message)

      override def warn(message: => String): F[Unit] =
        logger.warn(message)

      override def info(message: => String): F[Unit] =
        logger.info(message)

      override def debug(message: => String): F[Unit] =
        logger.debug(message)

      override def trace(message: => String): F[Unit] =
        logger.trace(message)

      override def error(t: Throwable)(message: => String): F[Unit] =
        logger.error(t)(message)

      override def warn(t: Throwable)(message: => String): F[Unit] =
        logger.warn(t)(message)

      override def info(t: Throwable)(message: => String): F[Unit] =
        logger.info(t)(message)

      override def debug(t: Throwable)(message: => String): F[Unit] =
        logger.debug(t)(message)

      override def trace(t: Throwable)(message: => String): F[Unit] =
        logger.trace(t)(message)

      def trace(ctx: Map[String, String])(msg: => String)(
          implicit
          name: sourcecode.FullName,
          line: sourcecode.Line
      ): F[Unit] = for {
        all <- context.getAll
        _ <- logger.trace(
          ctx
            .withSource(name, line)
            .withChatId(all)
            .withTrackingId(all)
        )(msg)
      } yield ()

      def trace(ctx: Map[String, String], t: Throwable)(msg: => String)(
          implicit
          name: sourcecode.FullName,
          line: sourcecode.Line
      ): F[Unit] = for {
        all <- context.getAll
        _ <- logger.trace(
          ctx
            .withSource(name, line)
            .withChatId(all)
            .withTrackingId(all),
          t
        )(msg)
      } yield ()

      def debug(ctx: Map[String, String])(msg: => String)(
          implicit
          name: sourcecode.FullName,
          line: sourcecode.Line
      ): F[Unit] = for {
        all <- context.getAll
        _ <- logger.debug(
          ctx
            .withSource(name, line)
            .withChatId(all)
            .withTrackingId(all)
        )(msg)
      } yield ()

      def debug(ctx: Map[String, String], t: Throwable)(msg: => String)(
          implicit
          name: sourcecode.FullName,
          line: sourcecode.Line
      ): F[Unit] = for {
        all <- context.getAll
        _ <- logger.debug(
          ctx
            .withSource(name, line)
            .withChatId(all)
            .withTrackingId(all),
          t
        )(msg)
      } yield ()

      def info(ctx: Map[String, String])(msg: => String)(
          implicit
          name: sourcecode.FullName,
          line: sourcecode.Line
      ): F[Unit] = for {
        all <- context.getAll
        _ <- logger.info(
          ctx
            .withSource(name, line)
            .withChatId(all)
            .withTrackingId(all)
        )(msg)
      } yield ()

      def info(ctx: Map[String, String], t: Throwable)(msg: => String)(
          implicit
          name: sourcecode.FullName,
          line: sourcecode.Line
      ): F[Unit] = for {
        all <- context.getAll
        _ <- logger.info(
          ctx
            .withSource(name, line)
            .withChatId(all)
            .withTrackingId(all)
        )(msg)
      } yield ()

      def warn(ctx: Map[String, String])(msg: => String)(
          implicit
          name: sourcecode.FullName,
          line: sourcecode.Line
      ): F[Unit] = for {
        all <- context.getAll
        _ <- logger.warn(
          ctx
            .withSource(name, line)
            .withChatId(all)
            .withTrackingId(all)
        )(msg)
      } yield ()

      def warn(ctx: Map[String, String], t: Throwable)(msg: => String)(
          implicit
          name: sourcecode.FullName,
          line: sourcecode.Line
      ): F[Unit] = for {
        all <- context.getAll
        _ <- logger.warn(
          ctx
            .withSource(name, line)
            .withChatId(all)
            .withTrackingId(all),
          t
        )(msg)
      } yield ()

      def error(ctx: Map[String, String])(msg: => String)(
          implicit
          name: sourcecode.FullName,
          line: sourcecode.Line
      ): F[Unit] = for {
        all <- context.getAll
        _ <- logger.error(
          ctx
            .withSource(name, line)
            .withChatId(all)
            .withTrackingId(all)
        )(msg)
      } yield ()

      def error(ctx: Map[String, String], t: Throwable)(msg: => String)(
          implicit
          name: sourcecode.FullName,
          line: sourcecode.Line
      ): F[Unit] = for {
        all <- context.getAll
        _ <- logger.error(
          ctx
            .withSource(name, line)
            .withChatId(all)
            .withTrackingId(all),
          t
        )(msg)
      } yield ()

      def error(message: => String)(
          implicit
          name: sourcecode.FullName,
          line: sourcecode.Line
      ): F[Unit] = for {
        all <- context.getAll
        _ <- logger.error(
          emptyLogContext
            .withSource(name, line)
            .withChatId(all)
            .withTrackingId(all)
        )(message)
      } yield ()

      def warn(message: => String)(
          implicit
          name: sourcecode.FullName,
          line: sourcecode.Line
      ): F[Unit] = for {
        all <- context.getAll
        _ <- logger.warn(
          emptyLogContext
            .withSource(name, line)
            .withChatId(all)
            .withTrackingId(all)
        )(message)
      } yield ()

      def info(message: => String)(
          implicit
          name: sourcecode.FullName,
          line: sourcecode.Line
      ): F[Unit] = for {
        all <- context.getAll
        _ <- logger.info(
          emptyLogContext
            .withSource(name, line)
            .withChatId(all)
            .withTrackingId(all)
        )(message)
      } yield ()

      def debug(message: => String)(
          implicit
          name: sourcecode.FullName,
          line: sourcecode.Line
      ): F[Unit] = for {
        all <- context.getAll
        _ <- logger.debug(
          emptyLogContext
            .withSource(name, line)
            .withChatId(all)
            .withTrackingId(all)
        )(message)
      } yield ()

      def trace(message: => String)(
          implicit
          name: sourcecode.FullName,
          line: sourcecode.Line
      ): F[Unit] = for {
        all <- context.getAll
        _ <- logger.trace(
          emptyLogContext
            .withSource(name, line)
            .withChatId(all)
            .withTrackingId(all)
        )(message)
      } yield ()

      def error(t: Throwable)(message: => String)(
          implicit
          name: sourcecode.FullName,
          line: sourcecode.Line
      ): F[Unit] = for {
        all <- context.getAll
        _ <- logger.error(
          emptyLogContext
            .withSource(name, line)
            .withChatId(all)
            .withTrackingId(all),
          t
        )(message)
      } yield ()

      def warn(t: Throwable)(message: => String)(
          implicit
          name: sourcecode.FullName,
          line: sourcecode.Line
      ): F[Unit] = for {
        all <- context.getAll
        _ <- logger.warn(
          emptyLogContext
            .withSource(name, line)
            .withChatId(all)
            .withTrackingId(all),
          t
        )(message)
      } yield ()

      def info(t: Throwable)(message: => String)(
          implicit
          name: sourcecode.FullName,
          line: sourcecode.Line
      ): F[Unit] = for {
        all <- context.getAll
        _ <- logger.info(
          emptyLogContext
            .withSource(name, line)
            .withChatId(all)
            .withTrackingId(all),
          t
        )(message)
      } yield ()

      def debug(t: Throwable)(message: => String)(
          implicit
          name: sourcecode.FullName,
          line: sourcecode.Line
      ): F[Unit] = for {
        all <- context.getAll
        _ <- logger.debug(
          emptyLogContext
            .withSource(name, line)
            .withChatId(all)
            .withTrackingId(all),
          t
        )(message)
      } yield ()

      def trace(t: Throwable)(message: => String)(
          implicit
          name: sourcecode.FullName,
          line: sourcecode.Line
      ): F[Unit] = for {
        all <- context.getAll
        _ <- logger.trace(
          emptyLogContext
            .withSource(name, line)
            .withChatId(all)
            .withTrackingId(all),
          t
        )(message)
      } yield ()
    }
  }
}
