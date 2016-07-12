package activity

import org.joda.time.DateTime

case class IntervalWithTrackingOption(interval: Long, timeStamp: Option[DateTime]) {
  override def toString: String = interval.toString + timeStamp.map(ts => s" ($ts)").getOrElse("")
}

case class SingleVarStats[A](min: A, mean: A, max: A) {
  def prettyPrint: String = s"min $min mean $mean max $max"
}

case class DimStats(varStats: SingleVarStats[Double], meanCrossingsCount: Int, meanCrossingsPercent: Double) {
  def prettyPrint: String = s"${varStats.prettyPrint} crossings: $meanCrossingsCount crossings%: $meanCrossingsPercent"
}

object DimStats {
  def apply(varStats: SingleVarStats[Double], measuredVars: Seq[Double]): DimStats =
  if(measuredVars.isEmpty) DimStats(varStats, 0, 0d)
  else {
    val (crossingsCount, _) = measuredVars.foldLeft((0, measuredVars.head)) { (acc, v) =>
      val (count, prev) = acc
      if(Math.signum(v - varStats.mean) != Math.signum(prev - varStats.mean)) (count + 1, v) else (count, v)
    }
    DimStats(varStats, crossingsCount, crossingsCount.toDouble / measuredVars.length)
  }
}

case class GroupStats(
                       length: Int,
                       x: DimStats,
                       y: DimStats,
                       z: DimStats,
                       intervals: SingleVarStats[IntervalWithTrackingOption],
                       durationMs: Long
                     ) {
  def prettyPrint: String =
    s"""
       |   rowcount: $length
       |   total duration: $durationMs ms
       |   x: ${x.prettyPrint}
       |   y: ${y.prettyPrint}
       |   z: ${z.prettyPrint}
       |   intervals: ${intervals.prettyPrint}""".stripMargin
}

object GroupStats {
  def apply(cells: Seq[MeasurementCell]): GroupStats = {
    val ln = cells.length
    val xs = cells.map(_.x)
    val ys = cells.map(_.y)
    val zs = cells.map(_.z)
    val ts = cells.init.zip(cells.tail).map { case (prev, next) => IntervalWithTrackingOption(next.time - prev.time, Some(new DateTime(prev.time))) }
    GroupStats(ln,
      DimStats(SingleVarStats(xs.min, xs.sum / ln, xs.max), xs),
      DimStats(SingleVarStats(ys.min, ys.sum / ln, ys.max), ys),
      DimStats(SingleVarStats(zs.min, zs.sum / ln, zs.max), zs),
      {
        if(ts.nonEmpty)
          SingleVarStats(ts.minBy(_.interval), IntervalWithTrackingOption(ts.map(_.interval).sum / ln, None), ts.maxBy(_.interval))
        else {
          val anystat = IntervalWithTrackingOption(-1L, None)
          SingleVarStats(anystat, anystat, anystat)
        }
      },
      if(cells.nonEmpty) cells.last.time - cells.head.time else 0L
    )
  }
}



