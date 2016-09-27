name := "common"

libraryDependencies ++= Seq(
  "org.apache.hadoop" % "hadoop-client" % Version.hadoop % "provided",
  "com.azavea.geotrellis" %% "geotrellis-vector" % "0.10.2",
  "com.github.scopt"  %% "scopt"        % "3.5.0"
)

fork in Test := false
parallelExecution in Test := false

initialCommands in console :=
  """
  import com.azavea.common._
  import org.opengis.feature.simple.SimpleFeatureType
  """
