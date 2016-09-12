package com.azavea.ca.server

import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus
import net.ceedubs.ficus.readers.ArbitraryTypeReader

trait Config {
  import Ficus._
  import ArbitraryTypeReader._

  protected case class HttpConfig(interface: String, port: Int)

  private val config = ConfigFactory.load()
  protected val httpConfig = config.as[HttpConfig]("http")
}
