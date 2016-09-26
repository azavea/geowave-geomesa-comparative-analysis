name := "mesa-plan"

resolvers ++= Seq(
  Resolver.mavenLocal,
  "locationtech-releases" at "https://repo.locationtech.org/content/repositories/releases/",
  "locationtech-snapshots" at "https://repo.locationtech.org/content/repositories/snapshots/"
)

libraryDependencies ++= Seq(
  "org.apache.accumulo" % "accumulo-core" % Version.accumulo
    exclude("org.jboss.netty", "netty")
    exclude("org.apache.hadoop", "hadoop-client"),
  "org.locationtech.geomesa" % "geomesa-accumulo-datastore" % Version.geomesa,
  "org.locationtech.geomesa" % "geomesa-utils" % Version.geomesa,
  "org.apache.hadoop" % "hadoop-client" % Version.hadoop
)

fork in Test := false
parallelExecution in Test := false
