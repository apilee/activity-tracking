package activity

import com.datastax.spark.connector.CassandraRow

case class MeasurementCell(time: String, x: Double, y: Double, z: Double)

case class MeasurementKey(userId: String, startTime: String, activity: String)

case class Measurement(key: MeasurementKey, cell: MeasurementCell)

case class MeasurementGroup(key: MeasurementKey, cells: Seq[MeasurementCell])

sealed trait MeasurementType {
  val name: String
  def apply(row: CassandraRow): Measurement
}

object Acceleration extends MeasurementType {
  val name = "acceleration"
  def apply(row: CassandraRow): Measurement =
    Measurement(
      MeasurementKey(row.getString("userid"), row.getString("starttime"), row.getString("activity")),
      MeasurementCell(row.getString("time"), row.getDouble("x"), row.getDouble("y"), row.getDouble("z"))
    )
}

object Orientation  extends MeasurementType {
  val name = "orientation"
  def apply(row: CassandraRow): Measurement =
    Measurement(
      MeasurementKey(row.getString("userid"), row.getString("starttime"), row.getString("activity")),
      MeasurementCell(row.getString("time"), row.getDouble("pitch"), row.getDouble("roll"), row.getDouble("yaw"))
    )
}

