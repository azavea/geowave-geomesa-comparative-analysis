import scala.util.Properties

object Version {
  def either(environmentVariable: String, default: String): String =
    Properties.envOrElse(environmentVariable, default)

  val geotrellis   = "0.10.2"
  val scala        = "2.11.8"
  val geotools     = "15.1"
  val geomesa      = "1.2.5"
  lazy val hadoop  = either("SPARK_HADOOP_VERSION", "2.6.0")
  lazy val spark   = either("SPARK_VERSION", "1.6.1")
}
