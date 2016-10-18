name := "common"

libraryDependencies ++= Seq(
  "com.azavea.geotrellis" %% "geotrellis-vector" % "0.10.2"
)

resourceDirectory in Compile := baseDirectory.value / "resources"

fork in Test := false
parallelExecution in Test := false
