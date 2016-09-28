name := "wave-plan"

resolvers ++= Seq(
  Resolver.mavenLocal,
  "geowave-release" at "http://geowave-maven.s3-website-us-east-1.amazonaws.com/release",
  "geowave-snapshot" at "http://geowave-maven.s3-website-us-east-1.amazonaws.com/snapshot"
)

libraryDependencies ++= Seq(
  "com.vividsolutions" % "jts-core" % Version.jts,
  "org.geotools" % "gt-coverage" % Version.geotools,
  "org.geotools" % "gt-epsg-hsql" % Version.geotools,
  "org.geotools" % "gt-geotiff" % Version.geotools,
  "org.geotools" % "gt-main" % Version.geotools,
  "org.geotools" % "gt-referencing" % Version.geotools,
  "mil.nga.giat" % "geowave-adapter-vector" % Version.geowave,
  "mil.nga.giat" % "geowave-core-store" % Version.geowave,
  "mil.nga.giat" % "geowave-datastore-accumulo" % Version.geowave
)

fork in Test := false
parallelExecution in Test := false
