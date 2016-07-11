package activity

import java.io.{File, PrintWriter}

import org.apache.spark.{SparkConf, SparkContext}
import com.datastax.spark.connector._

case class Acceleration(userId: String, startTime: String, time: String, activity: String, x: Double, y: Double, z: Double)

object Main {
  val writer = new PrintWriter(new File("output.txt"))

  def run(cassandraHostIP: String) = {
    val conf = new SparkConf(true).set("spark.cassandra.connection.host", cassandraHostIP)
    val sc = new SparkContext("local[*]", "spark-activity-tracking", conf)
    val accRdd = sc.cassandraTable("activitytracking", "trainingacceleration").map(r =>
      Acceleration(r.getString("userid"), r.getString("starttime"), r.getString("time"), r.getString("activity"), r.getDouble("x"), r.getDouble("y"), r.getDouble("z"))
    )

    writer.println(s"acceleration count: ${accRdd.count}")

    val accGroupRdd = accRdd.groupBy(a => (a.userId, a.startTime, a.activity))

    writer.println(s"acceleration measurements count: ${accGroupRdd.keys.count}")
    accGroupRdd.mapValues(_.toSeq.length).foreach { group =>
      writer.println(s"acceleration measurements: $group")
    }
  }

  def main(args: Array[String]): Unit = {
    try {
      run(args(0))
    } finally {
      writer.close()
    }
  }
}

