package org.github.ainr.telegram.reaction

trait BotReactionsInterpreter[F[_]] {

  def interpret(reactions: List[Reaction]): F[Unit]
}
