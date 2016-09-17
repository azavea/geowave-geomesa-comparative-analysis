name          := "explore"

libraryDependencies ++= {

  Seq(
    "org.apache.spark" %% "spark-core" % Version.spark % "provided",
    "org.apache.hadoop" % "hadoop-client" % Version.hadoop % "provided",

    "org.locationtech.geomesa" % "geomesa-compute" % Version.geomesa
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
