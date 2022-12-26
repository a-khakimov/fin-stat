package org.github.ainr.graphs

import cats.Parallel
import cats.syntax.all._
import cats.effect.{Clock, IO}
import cats.effect.kernel.Async
import org.nspl
import org.nspl.awtrenderer
import org.nspl.awtrenderer._

import java.io.File

trait Graphs[F[_]] {

  def plot(input: Input): F[File]
}

object Graphs {

  def apply[F[_]: Async: Parallel](): Graphs[F] = new Graphs[F] {

    override def plot(input: Input): F[File] = for {
      plots <- input.data
        .parTraverse {
          data => getPlot(data.seq, data.main, data.xlab, data.ylab, data.color)
        }
      sequence <- Async[F].delay(
        nspl.sequence(
          plots,
          nspl.TableLayout(plots.size)
        )
      )
      realTime <- Clock[F].realTime
      file <- Async[F].delay(
        awtrenderer.renderToFile(
          f = File.createTempFile(
            s"nspl_plot_${realTime.toMicros}",
            ".png"
          ),
          elem = sequence,
          width = 2000,
          mimeType = "image/png"
        )
      )
    } yield file

    private def getPlot(
        data: Seq[Double],
        main: String,
        xlab: String,
        ylab: String,
        fill: nspl.Colormap = nspl.Color.white,
    ): F[nspl.Elems2[nspl.XYPlotArea, nspl.Legend]] = Async[F].delay {
      nspl.xyplot(
        data -> nspl.line(
          stroke = nspl.StrokeConf(nspl.RelFontSize(0.1), nspl.CapRound),
          color = fill
        )
      )(parameters =
        nspl.par(
          main = main,
          xlab = xlab,
          ylab = ylab
        )
      ).build
    }
  }
}
