lazy val commonSettings = Seq(
  version := "0.1.14",
  scalaVersion := Version.scala,
  crossScalaVersions := Seq("2.11.8", "2.10.5"),
  organization := "com.azavea",
  licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
  scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-Yinline-warnings",
    "-language:implicitConversions",
    "-language:reflectiveCalls",
    "-language:higherKinds",
    "-language:postfixOps",
    "-language:existentials",
    "-feature"),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },

  resolvers ++= Seq(
    "java3d" at "http://maven.geotoolkit.org/",
    "geosolutions" at "http://maven.geo-solutions.it/",
    "osgeo" at "http://download.osgeo.org/webdav/geotools/",
    "maven" at "http://central.maven.org/maven2/",
    Resolver.sonatypeRepo("public")
  ),

  libraryDependencies ++= Seq(
    "com.googlecode.efficient-java-matrix-library" % "ejml" % "0.25",
    "com.vividsolutions"   %  "jts"            % "1.13",
    "net.java.dev.jsr-275" % "jsr-275"         % "1.0-beta-2",
    "org.geotools"         % "gt-data"         % Version.geotools,
    "org.geotools"         % "gt-shapefile"    % Version.geotools,
    "org.geotools"         % "gt-api"          % Version.geotools,
    "org.geotools"         % "gt-main"         % Version.geotools,
    "org.geotools"         % "gt-metadata"     % Version.geotools,
    "org.geotools"         % "gt-opengis"      % Version.geotools,
    "org.geotools"         % "gt-referencing"  % Version.geotools,
    "org.geotools"         % "gt-epsg-hsql"    % Version.geotools,
    "com.github.scopt"     %% "scopt"          % "3.5.0"
  )
)

// When creating fat jar, remote some files with
// bad signatures and resolve conflicts by taking the first
// versions of shared packaged types.
assemblyMergeStrategy in assembly := {
  case "reference.conf" => MergeStrategy.concat
  case "application.conf" => MergeStrategy.concat
  case "META-INF/MANIFEST.MF" => MergeStrategy.discard
  case "META-INF\\MANIFEST.MF" => MergeStrategy.discard
  case "META-INF/ECLIPSEF.RSA" => MergeStrategy.discard
  case "META-INF/ECLIPSEF.SF" => MergeStrategy.discard
  case _ => MergeStrategy.first
}

lazy val root = Project("shapefile-ingest", file("."))
  .settings(commonSettings: _*)
  .settings(name := "shapefile-ingest")
  .aggregate(geomesaIngest)

lazy val common = Project("common", file("common"))
  .settings(commonSettings: _*)

lazy val geomesaIngest = Project("geomesa", file("geomesa"))
  .settings(commonSettings: _*)
  .dependsOn(common)

lazy val geowaveIngest = Project("geowave", file("geowave"))
  .settings(commonSettings: _*)
  .dependsOn(common)
