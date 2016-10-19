package com.azavea.ca.synthetic

import scopt._
import scala.util._
import java.util.HashMap

object CommandLine {
  case class Options(
    instanceId: String,
    zookeepers: String,
    user: String,
    password: String,
    tableName: String,
    index: String = null,
    instructions: List[String] = List.empty
  ) {
    def dataSourceConf: HashMap[String, String] = {
      val dsConf = new java.util.HashMap[String, String]()
      dsConf.put("instanceId", instanceId)
      dsConf.put("zookeepers", zookeepers)
      dsConf.put("user", user)
      dsConf.put("password", password)
      dsConf.put("tableName", tableName)
      dsConf
    }
  }

  val DEFAULT_OPTIONS = Options("gis", "localhost", "root", "secret", "features")

  val parser = new OptionParser[Options]("synthetic-data") {
    // for debugging; prevents exit from sbt console
    override def terminate(exitState: Either[String, Unit]): Unit = ()

    opt[String]('i', "instance")
      .action( (s, conf) => conf.copy(instanceId = s))
      .text("Accumulo instance name")
      .required
    opt[String]('z',"zookeepers")
      .action( (s, conf) => conf.copy(zookeepers = s) )
      .text("Comma-separated list of zookeepers [default=zookeeper]")
      .required
    opt[String]('u',"user")
      .action( (s, conf) => conf.copy(user = s) )
      .text("User namer [default=root]")
      .required
    opt[String]('p',"password")
      .action( (s, conf) => conf.copy(password = s) )
      .text("Password [default=GisPwd]")
      .required
    opt[String]('t',"table")
      .action( (s, conf) => conf.copy(tableName = s) )
      .text("Table name for writing features")
      .required
    opt[String]("index").optional()
      .action( (x, c) => c.copy(index = x))
      .text("Specify index to build (ex: 'space', 'space:xbits:ybits', 'spacetime', 'spacetime:xbits:ybits:tbits')")
    arg[String]("<instruction>...").unbounded().optional()
      .action( (x, c) => c.copy(instructions = c.instructions :+ x) )
      .text("optional unbounded instructions")
  }
}
