package activity

import org.joda.time.DateTime

case class IntervalWithTrackingOption(interval: Long, timeStamp: Option[DateTime]) {
  override def toString: String = interval.toString + timeStamp.map(ts => s" ($ts)").getOrElse("")
}

case class SingleVarStats[A](min: A, mean: A, max: A) {
  def prettyPrint: String = s"min $min mean $mean max $max"
}

case class GroupStats(length: Int, x: SingleVarStats[Double], y: SingleVarStats[Double], z: SingleVarStats[Double], intervals: SingleVarStats[IntervalWithTrackingOption]) {
  def prettyPrint: String =
    s"""
       |   length: $length
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
      SingleVarStats(xs.min, xs.sum / ln, xs.max),
      SingleVarStats(ys.min, ys.sum / ln, ys.max),
      SingleVarStats(zs.min, zs.sum / ln, zs.max),
      {
        if(ts.nonEmpty)
          SingleVarStats(ts.minBy(_.interval), IntervalWithTrackingOption(ts.map(_.interval).sum / ln, None), ts.maxBy(_.interval))
        else {
          val anystat = IntervalWithTrackingOption(-1L, None)
          SingleVarStats(anystat, anystat, anystat)
        }
      }
    )
  }
}



