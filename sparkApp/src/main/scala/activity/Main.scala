package activity

import java.io.{File, PrintWriter}

import org.apache.spark.{SparkConf, SparkContext}
import com.datastax.spark.connector._

object Main {
  val writer = new PrintWriter(new File("output.txt"))

  def run(cassandraHostIP: String) = {
    val conf = new SparkConf(true).set("spark.cassandra.connection.host", cassandraHostIP)
    val sc = new SparkContext("local[*]", "spark-activity-tracking", conf)
    val rdd = sc.cassandraTable("activitytracking", "trainingorientation")
    writer.println("111111111111111111111111")
    writer.println(s"rdd.count ${rdd.count()}")
    writer.println(s"rdd.first ${rdd.first()}")
    writer.println(s"yaw sum " + rdd.map(_.getInt("yaw")).sum)
  }

  def main(args: Array[String]): Unit = {
    try {
      run(args(0))
    } finally {
      writer.close()
    }
  }
}

