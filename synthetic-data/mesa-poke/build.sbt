name := "mesa-poke"

resolvers ++= Seq(
  // "locationtech-releases" at "https://repo.locationtech.org/content/repositories/releases/",
  // "locationtech-snapshots" at "https://repo.locationtech.org/content/repositories/snapshots/",
  "Local Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository",
  "boundlessgeo" at "http://repo.boundlessgeo.com/main/"
)

libraryDependencies ++= Seq(
  "org.apache.hadoop" % "hadoop-client" % Version.hadoop % "provided",
  "org.apache.spark" %% "spark-core" % Version.spark % "provided",
  "org.locationtech.geomesa" % "geomesa-accumulo-datastore" % Version.geomesa,
  "org.locationtech.geomesa" % "geomesa-utils" % Version.geomesa
)

fork in Test := false
parallelExecution in Test := false
