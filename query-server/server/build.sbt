name          := "query-server"

libraryDependencies ++= {
  Seq(
    "com.amazonaws" % "aws-java-sdk" % "1.11.31",
    "com.typesafe.akka" %% "akka-http-core"                    % Version.akka,
    "com.typesafe.akka" %% "akka-http-experimental"            % Version.akka,
    "com.iheart"        %% "ficus"                             % Version.ficus,
    "io.circe"          %% "circe-core"                        % Version.circe,
    "io.circe"          %% "circe-generic"                     % Version.circe,
    "io.circe"          %% "circe-parser"                      % Version.circe,
    "de.heikoseeberger" %% "akka-http-circe"                   % Version.akkaCirce,
    "org.geotools"      %  "gt-geotiff"                        % Version.geotools,
    "org.scalatest"     %% "scalatest"                         % Version.scalaTest  % "test",
    "org.scalamock"     %% "scalamock-scalatest-support"       % Version.scalaMock  % "test",
    "com.typesafe.akka" %% "akka-http-testkit"                 % Version.akka       % "test",
    "org.locationtech.geomesa"  % "geomesa-accumulo-datastore" % Version.geomesa,
    "javax.media"       %  "jai_core"                          % Version.jai
      from "https://s3.amazonaws.com/geowave-geomesa-comparison/jars/jai_core-1.1.3.jar",
    "org.apache.accumulo" % "accumulo-core"                    % Version.accumulo
      exclude("org.jboss.netty", "netty")
      exclude("org.apache.hadoop", "hadoop-client"),
    "mil.nga.giat" % "geowave-adapter-raster" % Version.geowave
      excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
        ExclusionRule(organization = "javax.servlet")),
    "mil.nga.giat" % "geowave-adapter-vector" % Version.geowave
      excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
        ExclusionRule(organization = "javax.servlet")),
    "mil.nga.giat" % "geowave-core-store" % Version.geowave
      excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
          ExclusionRule(organization = "javax.servlet")),
    "mil.nga.giat" % "geowave-datastore-accumulo" % Version.geowave
      excludeAll(ExclusionRule(organization = "org.mortbay.jetty"),
        ExclusionRule(organization = "javax.servlet")),
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
}

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
