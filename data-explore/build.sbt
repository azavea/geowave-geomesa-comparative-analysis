name          := "data-explore"
organization  := "com.azavea"
version       := "0.0.1"
scalaVersion  := "2.11.8"

resolvers ++= Seq(
  "boundless" at "https://repo.boundlessgeo.com/release",
  "boundlessgeo" at "https://boundless.artifactoryonline.com/boundless/main",
  "geomesa releases" at "https://repo.locationtech.org/content/repositories/releases/",
  "geomesa snapshots" at "https://repo.locationtech.org/content/repositories/snapshots/",
  "geosolutions" at "http://maven.geo-solutions.it/",
  "geowave release" at "http://geowave-maven.s3-website-us-east-1.amazonaws.com/release",
  "geowave snapshot" at "http://geowave-maven.s3-website-us-east-1.amazonaws.com/snapshot",
  "osgeo" at "http://download.osgeo.org/webdav/geotools/",
  "sfcurve releases" at "https://repo.locationtech.org/content/repositories/sfcurve-releases"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "utf8",
  "-feature",
  "-language:existentials",
  "-language:experimental.macros",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-language:reflectiveCalls",
  "-unchecked"
)

libraryDependencies ++= {
  val akkaV            = "2.4.8"
  val ficusV           = "1.2.4"
  val circeV           = "0.4.1"
  val scalaTestV       = "3.0.0-M4"
  val scalaMockV       = "3.2.2"
  val scalazScalaTestV = "0.3.0"
  val akkaCirceV       = "1.8.0"
  val geomesaV         = "1.2.6"
  val geotoolsV        = "14.3"
  val sfcurveV         = "0.2.0"
  val jaiV             = "1.1.3"
  val accumuloV        = "1.7.1"

  lazy val hadoopV  = "2.6.2"
  lazy val sparkV   = "2.0.0"

  Seq(
    "org.apache.spark" %% "spark-core" % sparkV % "provided",
    "org.apache.hadoop" % "hadoop-client" % hadoopV % "provided",

    "com.azavea.geotrellis" %% "geotrellis-vector" % "0.10.2",

    "org.locationtech.geomesa" % "geomesa-accumulo-datastore" % geomesaV,
    "org.locationtech.geomesa" % "geomesa-utils" % geomesaV,
    "org.locationtech.geomesa" % "geomesa-compute" % geomesaV,

    "org.apache.hadoop" % "hadoop-client" % hadoopV % "provided",
    "org.apache.accumulo" % "accumulo-core" % accumuloV
      excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
        ExclusionRule(organization = "org.apache.hadoop")
      ),
    "org.geotools" % "gt-coverage" % geotoolsV
      excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
        ExclusionRule(organization = "javax.servlet")),
    "org.geotools" % "gt-epsg-hsql" % geotoolsV
      excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
        ExclusionRule(organization = "javax.servlet")),
    "org.geotools" % "gt-geotiff" % geotoolsV
      excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
        ExclusionRule(organization = "javax.servlet")),
    "org.geotools" % "gt-main" % geotoolsV
      excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
        ExclusionRule(organization = "javax.servlet")),
    "org.geotools" % "gt-referencing" % geotoolsV
      excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
        ExclusionRule(organization = "javax.servlet"))
  )
}

lazy val root = project.in(file("."))

publishArtifact in Test := false
pomIncludeRepository := { _ => false }

// When creating fat jar, remove some files with
// bad signatures and resolve conflicts by taking the first
// versions of shared packaged types.
assemblyMergeStrategy in assembly := {
  case "reference.conf" => MergeStrategy.concat
  case "application.conf" => MergeStrategy.concat
  case "META-INF/MANIFEST.MF" => MergeStrategy.discard
  case "META-INF\\MANIFEST.MF" => MergeStrategy.discard
  case "META-INF/ECLIPSEF.RSA" => MergeStrategy.discard
  case "META-INF/ECLIPSEF.SF" => MergeStrategy.discard
  case "META-INF/BCKEY.SF" => MergeStrategy.discard
  case "META-INF/BCKEY.DSA" => MergeStrategy.discard
  case x if x.startsWith("META-INF/services") => MergeStrategy.concat
  case _ => MergeStrategy.first
}
