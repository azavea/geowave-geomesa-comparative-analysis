name := "mesa-poke"

resolvers ++= Seq(
  "locationtech-releases" at "https://repo.locationtech.org/content/repositories/releases/",
  "locationtech-snapshots" at "https://repo.locationtech.org/content/repositories/snapshots/",
  "boundlessgeo" at "http://repo.boundlessgeo.com/main/"
)

libraryDependencies ++= Seq(
  "org.apache.hadoop" % "hadoop-client" % Version.hadoop % "provided",
  "org.apache.spark" %% "spark-core" % Version.spark % "provided",
  "org.locationtech.geomesa" % "geomesa-accumulo-datastore" % Version.geomesa,
  "org.locationtech.geomesa" % "geomesa-utils" % Version.geomesa,
  "org.locationtech.geomesa" % "geomesa-jobs" % Version.geomesa
)

fork in Test := false
parallelExecution in Test := false
