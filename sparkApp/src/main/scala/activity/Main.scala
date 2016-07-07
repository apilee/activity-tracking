package activity

import org.apache.spark.{SparkConf, SparkContext}
import com.datastax.spark.connector._

object Main {
  def run(cassandraHostIP: String) = {
    val conf = new SparkConf(true).set("spark.cassandra.connection.host", cassandraHostIP)
    val sc = new SparkContext("local[*]", "spark-activity-tracking", conf)
    val rdd = sc.cassandraTable("activitytracking", "trainingorientation")
    println("111111111111111111111111")
    println(s"rdd.count ${rdd.count()}")
    println(s"rdd.first ${rdd.first()}")
    println(s"yaw sum " + rdd.map(_.getInt("yaw")).sum)
  }

  def main(args: Array[String]): Unit = run(args(0))
}

