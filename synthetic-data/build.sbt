import sbtassembly.PathList

val generalDependencies = Seq(
  "org.geotools" % "gt-coverage" % Version.geotools,
  "org.geotools" % "gt-epsg-hsql" % Version.geotools,
  "org.geotools" % "gt-geotiff" % Version.geotools,
  "org.geotools" % "gt-main" % Version.geotools,
  "org.geotools" % "gt-referencing" % Version.geotools,
  "com.vividsolutions" % "jts-core" % Version.jts,
  "org.apache.accumulo" % "accumulo-core" % Version.accumulo
    exclude("org.jboss.netty", "netty")
    exclude("org.apache.hadoop", "hadoop-client"),
  "org.apache.hadoop" % "hadoop-client" % Version.hadoop
)

val extraResolvers = Seq(
  "geotools" at "http://download.osgeo.org/webdav/geotools/"
)

lazy val commonSettings = Seq(
  organization := "com.azavea",
  version := "0",
  scalaVersion := "2.10.6",
  test in assembly := {},
  assemblyMergeStrategy in assembly := {
    case "reference.conf" => MergeStrategy.concat
    case "application.conf" => MergeStrategy.concat
    case PathList("META-INF", xs @ _*) =>
      xs match {
        case ("MANIFEST.MF" :: Nil) => MergeStrategy.discard
          // Concatenate everything in the services directory to keep
          // GeoTools happy.
        case ("services" :: _ :: Nil) =>
          MergeStrategy.concat
          // Concatenate these to keep JAI happy.
        case ("javax.media.jai.registryFile.jai" :: Nil) | ("registryFile.jai" :: Nil) | ("registryFile.jaiext" :: Nil) =>
          MergeStrategy.concat
        case (name :: Nil) => {
          // Must exclude META-INF/*.([RD]SA|SF) to avoid "Invalid
          // signature file digest for Manifest main attributes"
          // exception.
          if (name.endsWith(".RSA") || name.endsWith(".DSA") || name.endsWith(".SF"))
            MergeStrategy.discard
          else
            MergeStrategy.first
        }
        case _ => MergeStrategy.first
      }
    case _ => MergeStrategy.first
  },
  shellPrompt := { s => Project.extract(s).currentProject.id + " > " }
)

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= generalDependencies)

lazy val common = (project in file("common"))
  .dependsOn(root)
  .settings(commonSettings: _*)

lazy val mesaPoke = (project in file("mesa-poke"))
  .dependsOn(root)
  .dependsOn(common)
  .settings(commonSettings: _*)
  .settings(resolvers ++= extraResolvers)
