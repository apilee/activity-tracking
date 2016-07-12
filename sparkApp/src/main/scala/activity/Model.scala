package activity

import com.datastax.spark.connector.CassandraRow
import org.joda.time.DateTime

case class MeasurementCell(time: Long, x: Double, y: Double, z: Double)

case class MeasurementKey(userId: String, startTime: DateTime, activity: String) {
  def prettyPrint: String = s"$userId $startTime $activity"
}

case class Measurement(key: MeasurementKey, cell: MeasurementCell)

case class MeasurementGroup(key: MeasurementKey, cells: Seq[MeasurementCell]) {
  def stats: (MeasurementKey, GroupStats) = (key, GroupStats(cells))
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

