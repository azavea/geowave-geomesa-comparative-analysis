name := "wave-poke"

resolvers ++= Seq(
  "geowave-release" at "http://geowave-maven.s3-website-us-east-1.amazonaws.com/release",
  "geowave-snapshot" at "http://geowave-maven.s3-website-us-east-1.amazonaws.com/snapshot"
  // "boundlessgeo" at "http://repo.boundlessgeo.com/main/"
)

libraryDependencies ++= Seq(
  "mil.nga.giat" % "geowave-adapter-vector" % Version.geowave,
  "mil.nga.giat" % "geowave-core-store" % Version.geowave,
  "mil.nga.giat" % "geowave-datastore-accumulo" % Version.geowave,
  "org.apache.hadoop" % "hadoop-client" % Version.hadoop % "provided",
  "org.apache.spark" %% "spark-core" % Version.spark % "provided",
  "org.locationtech.geomesa" % "geomesa-utils" % Version.geomesa // sic!
)

fork in Test := false
parallelExecution in Test := false
