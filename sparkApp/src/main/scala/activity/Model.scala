package activity

import com.datastax.spark.connector.CassandraRow

sealed trait Measurement {
  val userId: String
  val startTime: String
  val activity: String
}

case class Acceleration(userId: String, startTime: String, time: String, activity: String, x: Double, y: Double, z: Double) extends Measurement

case class Orientation(userId: String, startTime: String, time: String, activity: String, pitch: Double, roll: Double, yaw: Double) extends Measurement

sealed trait MeasurementType {
  val name: String
  def apply(row: CassandraRow): Measurement
}

object Acceleration extends MeasurementType {
  val name = "acceleration"
  def apply(row: CassandraRow): Measurement =
    Acceleration(row.getString("userid"), row.getString("starttime"), row.getString("time"), row.getString("activity"),
      row.getDouble("x"), row.getDouble("y"), row.getDouble("z"))
}

object Orientation  extends MeasurementType {
  val name = "orientation"
  def apply(row: CassandraRow): Measurement =
    Orientation(row.getString("userid"), row.getString("starttime"), row.getString("time"), row.getString("activity"),
      row.getDouble("pitch"), row.getDouble("roll"), row.getDouble("yaw"))
}

