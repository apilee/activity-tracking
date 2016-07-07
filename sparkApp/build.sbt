name         := "spark-activity-tracking"
version      := "1.0"

scalaVersion := "2.10.6"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "1.6.1" % "provided",
  "org.apache.spark" %% "spark-sql" % "1.6.1" % "provided",
  "com.datastax.spark" %% "spark-cassandra-connector" % "1.6.0"
)

assemblyMergeStrategy in assembly := {
  case x if x.endsWith(".class") => MergeStrategy.last
  case x if x.endsWith(".properties") => MergeStrategy.last
  case x if x.contains("/resources/") => MergeStrategy.last
  case x if x.startsWith("META-INF/mailcap") => MergeStrategy.last
  case x if x.startsWith("META-INF/mimetypes.default") => MergeStrategy.first
  case x if x.startsWith("META-INF/maven/org.slf4j/slf4j-api/pom.") => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    if (oldStrategy == MergeStrategy.deduplicate)
      MergeStrategy.first
    else
      oldStrategy(x)
}
