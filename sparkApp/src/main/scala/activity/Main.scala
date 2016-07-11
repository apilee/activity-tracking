package activity

import java.io.{File, PrintWriter}

import org.apache.spark.{SparkConf, SparkContext}
import com.datastax.spark.connector._



object Main {
  val writer = new PrintWriter(new File("output.txt"))

  def preAnalysis(sc: SparkContext, measurementType: MeasurementType) = {
    val rawRdd = sc.cassandraTable("activitytracking", s"training${measurementType.name}").map(measurementType.apply)

    writer.println(s"${measurementType.name} count: ${rawRdd.count}")

    val grouppedRdd = rawRdd.groupBy(a => (a.userId, a.startTime, a.activity))

    writer.println(s"${measurementType.name} measurements count: ${grouppedRdd.keys.count}")
    grouppedRdd.mapValues(_.toSeq.length).foreach { group =>
      writer.println(s"${measurementType.name} measurements: $group")
    }
  }

  def run(cassandraHostIP: String) = {
    val conf = new SparkConf(true).set("spark.cassandra.connection.host", cassandraHostIP)
    val sc = new SparkContext("local[*]", "spark-activity-tracking", conf)
    preAnalysis(sc, Acceleration)
    preAnalysis(sc, Orientation)
  }

  def main(args: Array[String]): Unit = {
    try {
      run(args(0))
    } finally {
      writer.close()
    }
  }
}

