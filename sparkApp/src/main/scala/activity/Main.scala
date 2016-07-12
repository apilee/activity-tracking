package activity

import java.io.{File, PrintWriter}

import org.apache.spark.{SparkConf, SparkContext}
import com.datastax.spark.connector._

object Main {
  val writer = new PrintWriter(new File("output.txt"))

  def preAnalysis(sc: SparkContext, measurementType: MeasurementType) = {
    val rawRdd = sc.cassandraTable("activitytracking", s"training${measurementType.name}")
    //moving from RDD to Scala Seq, because it's easier to process and we don't have lots of data currently
    val measurements = rawRdd.collect().map(measurementType.apply)

    writer.println("sample: " + measurements.take(15).toList)

    writer.println(s"${measurementType.name} count: ${measurements.length}")

    val groups = measurements.groupBy(a => a.key).map {
      case (key, rows) =>
        MeasurementGroup(key, rows.toSeq.map(r => r.cell))
    }.toSeq

    writer.println(s"${measurementType.name} measurements count: ${groups.length}")
    groups.map(_.stats).foreach { group =>
      writer.println(s"${measurementType.name} measurements: ${group._1} ${group._2.prettyPrint}")
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

