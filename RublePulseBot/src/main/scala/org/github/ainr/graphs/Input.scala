package org.github.ainr.graphs

import org.github.ainr.graphs.Input.Data
import org.nspl

final case class Input(data: List[Data])

object Input {
  case class Data(
      seq: Seq[Double],
      main: String,
      xlab: String,
      ylab: String,
      color: nspl.Colormap
  )
}
