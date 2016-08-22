name := "wave-poke"

resolvers ++= Seq(
  "geowave-release" at "http://geowave-maven.s3-website-us-east-1.amazonaws.com/release",
  "geowave-snapshot" at "http://geowave-maven.s3-website-us-east-1.amazonaws.com/snapshot"
)

libraryDependencies ++= Seq(
  "mil.nga.giat" % "geowave-adapter-vector" % Version.geowave,
  "mil.nga.giat" % "geowave-core-store" % Version.geowave,
  "mil.nga.giat" % "geowave-datastore-accumulo" % Version.geowave,
  "org.apache.hadoop" % "hadoop-client" % Version.hadoop % "provided",
  "org.apache.spark" %% "spark-core" % Version.spark % "provided"
)

fork in Test := false
parallelExecution in Test := false
