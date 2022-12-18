package org.github.ainr.tinvest

import cats.syntax.all._
import cats.effect.Sync
import io.grpc.Metadata
import io.grpc.Metadata.AsciiMarshaller

object AuthMetadata {

  private val authKey: Metadata.Key[String] =
    Metadata.Key.of("Authorization", StringMarshaller)

  def apply[F[_]: Sync](token: String): F[Metadata] = {
    Sync[F]
      .delay(new Metadata())
      .map {
        metadata =>
          metadata.put(authKey, s"Bearer $token")
          metadata
      }
  }

}

case object StringMarshaller extends AsciiMarshaller[String] {
  override def toAsciiString(value: String): String = value

  override def parseAsciiString(serialized: String): String = serialized
}
