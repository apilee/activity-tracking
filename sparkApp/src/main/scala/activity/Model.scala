package activity

import com.datastax.spark.connector.CassandraRow
import org.joda.time.DateTime

case class MeasurementCell(time: Long, x: Double, y: Double, z: Double)

case class SingleVarStats[A](min: A, mean: A, max: A) {
  def prettyPrint: String = s"$min $mean $max"
}

case class MeasurementKey(userId: String, startTime: DateTime, activity: String)

case class Measurement(key: MeasurementKey, cell: MeasurementCell)

case class GroupStats(length: Int, x: SingleVarStats[Double], y: SingleVarStats[Double], z: SingleVarStats[Double], intervals: SingleVarStats[Long]) {
  def prettyPrint: String =
    s"length: $length \n   x: ${x.prettyPrint} \n   y: ${y.prettyPrint} \n   z: ${z.prettyPrint} \n   intervals: ${intervals.prettyPrint}"
}

case class MeasurementGroup(key: MeasurementKey, cells: Seq[MeasurementCell]) {
  def stats: (MeasurementKey, GroupStats) = {
    val ln = cells.length
    val xs = cells.map(_.x)
    val ys = cells.map(_.y)
    val zs = cells.map(_.z)
    val ts = cells.init.zip(cells.tail).map { case (prev, next) => next.time - prev.time }
    val st = GroupStats(ln,
      SingleVarStats(xs.min, xs.sum / ln, xs.max),
      SingleVarStats(ys.min, ys.sum / ln, ys.max),
      SingleVarStats(zs.min, zs.sum / ln, zs.max),
      if(ts.nonEmpty) SingleVarStats(ts.min, ts.sum / ln, ts.max) else SingleVarStats(-1, -1, -1)
    )
    (key, st)
  }
}

sealed trait MeasurementType {
  val name: String
  def apply(row: CassandraRow): Measurement
}

object Acceleration extends MeasurementType {
  val name = "acceleration"
  def apply(row: CassandraRow): Measurement =
    Measurement(
      MeasurementKey(row.getString("userid"), row.getDateTime("starttime"), row.getString("activity")),
      MeasurementCell(row.getDateTime("time").getMillis, row.getDouble("x"), row.getDouble("y"), row.getDouble("z"))
    )
}

object Orientation  extends MeasurementType {
  val name = "orientation"
  def apply(row: CassandraRow): Measurement =
    Measurement(
      MeasurementKey(row.getString("userid"), row.getDateTime("starttime"), row.getString("activity")),
      MeasurementCell(row.getDateTime("time").getMillis, row.getDouble("pitch"), row.getDouble("roll"), row.getDouble("yaw"))
    )
}

