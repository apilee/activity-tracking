package activity

import com.datastax.spark.connector.CassandraRow
import org.joda.time.DateTime

case class MeasurementCell(time: DateTime, x: Double, y: Double, z: Double)

case class MeasurementVector(min: Double, mean: Double, max: Double) {
  def prettyPrint: String = s"$min $mean $max"
}

case class MeasurementKey(userId: String, startTime: DateTime, activity: String)

case class Measurement(key: MeasurementKey, cell: MeasurementCell)

case class GroupStats(length: Int, x: MeasurementVector, y: MeasurementVector, z: MeasurementVector) {
  def prettyPrint: String =
    s"length: $length \n   x: ${x.prettyPrint} \n   y: ${y.prettyPrint} \n   z: ${z.prettyPrint}"
}

case class MeasurementGroup(key: MeasurementKey, cells: Seq[MeasurementCell]) {
  def stats: (MeasurementKey, GroupStats) = {
    val ln = cells.length
    val xs = cells.map(_.x)
    val ys = cells.map(_.y)
    val zs = cells.map(_.z)
    val st = GroupStats(ln, MeasurementVector(xs.min, xs.sum / ln, xs.max), MeasurementVector(ys.min, ys.sum / ln, ys.max), MeasurementVector(zs.min, zs.sum / ln, zs.max))
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
      MeasurementCell(row.getDateTime("time"), row.getDouble("x"), row.getDouble("y"), row.getDouble("z"))
    )
}

object Orientation  extends MeasurementType {
  val name = "orientation"
  def apply(row: CassandraRow): Measurement =
    Measurement(
      MeasurementKey(row.getString("userid"), row.getDateTime("starttime"), row.getString("activity")),
      MeasurementCell(row.getDateTime("time"), row.getDouble("pitch"), row.getDouble("roll"), row.getDouble("yaw"))
    )
}

