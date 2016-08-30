import sbt._
import scala.util.Properties

object Version {
  def either(environmentVariable: String, default: String): String = Properties.envOrElse(environmentVariable, default)

  lazy val hadoop  = either("SPARK_HADOOP_VERSION", "2.6.2")
  lazy val spark   = either("SPARK_VERSION", "1.6.1")

  val accumulo = "1.7.1"
  val geomesa  = "1.2.5"
  val geowave  = "0.9.3-SNAPSHOT"
  val geotools = "14.3"
  val jts      = "1.14.0"
}
