lazy val commonSettings = Seq(
  version := "0.0.1",
  scalaVersion := "2.11.8",
  organization := "com.azavea",
  licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
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
  ),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  shellPrompt := { s => Project.extract(s).currentProject.id + " > " },

  resolvers ++= Seq(
    "boundless" at "https://repo.boundlessgeo.com/release",
    "boundlessgeo" at "https://boundless.artifactoryonline.com/boundless/main",
    "geomesa releases" at "https://repo.locationtech.org/content/repositories/releases/",
    "geomesa snapshots" at "https://repo.locationtech.org/content/repositories/snapshots/",
    "geosolutions" at "http://maven.geo-solutions.it/",
    "geowave release" at "http://geowave-maven.s3-website-us-east-1.amazonaws.com/release",
    "geowave snapshot" at "http://geowave-maven.s3-website-us-east-1.amazonaws.com/snapshot",
    "osgeo" at "http://download.osgeo.org/webdav/geotools/",
    "sfcurve releases" at "https://repo.locationtech.org/content/repositories/sfcurve-releases",
    Resolver.bintrayRepo("hseeberger", "maven"),
    Resolver.jcenterRepo
  ),


  libraryDependencies ++= Seq(
    "com.googlecode.efficient-java-matrix-library" % "ejml" % "0.25",
    "com.vividsolutions"   %  "jts"            % "1.13",
    "net.java.dev.jsr-275" % "jsr-275"         % "1.0-beta-2",
    "org.geoserver" % "gs-wms" % "2.8.2"
      excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
        ExclusionRule(organization = "javax.servlet")),
    "org.geotools" % "gt-coverage" % Version.geotools
      excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
        ExclusionRule(organization = "javax.servlet")),
    "org.geotools" % "gt-epsg-hsql" % Version.geotools
      excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
        ExclusionRule(organization = "javax.servlet")),
    "org.geotools" % "gt-geotiff" % Version.geotools
      excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
        ExclusionRule(organization = "javax.servlet")),
    "org.geotools" % "gt-main" % Version.geotools
      excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
        ExclusionRule(organization = "javax.servlet")),
    "org.geotools" % "gt-referencing" % Version.geotools
      excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
        ExclusionRule(organization = "javax.servlet"))
  )
)

lazy val root = Project("aggregate", file("."))
  .settings(name := "aggregate")
  .aggregate(core, explore, server)

lazy val core = Project("core", file("core"))
  .settings(commonSettings: _*)

lazy val server = Project("server", file("server"))
  .settings(commonSettings: _*)
  .dependsOn(core)

lazy val explore = Project("explore", file("explore"))
  .settings(commonSettings: _*)
  .dependsOn(server)
