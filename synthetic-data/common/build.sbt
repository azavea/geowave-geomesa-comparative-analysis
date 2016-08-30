name := "common"

libraryDependencies ++= Seq(
  "org.apache.hadoop" % "hadoop-client" % Version.hadoop % "provided"
)

fork in Test := false
parallelExecution in Test := false

initialCommands in console :=
  """
  import com.azavea.common._
  import org.opengis.feature.simple.SimpleFeatureType
  """
