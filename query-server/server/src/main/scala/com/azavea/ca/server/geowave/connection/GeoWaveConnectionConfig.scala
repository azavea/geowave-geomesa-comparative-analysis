package com.azavea.ca.server.geowave.connection

import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus
import net.ceedubs.ficus.readers.ArbitraryTypeReader

case class GeoWaveConnectionConfig(
  user: String,
  password: String,
  instance: String,
  zookeepers: String,
  cluster: String
) {
  def toParamsMap(gwNamespace: String) = {
    println(s"Creating GeoWave connection with: table->${gwNamespace}, user->${this.user}, password->${this.password}, instance->${this.instance}, and zk->${this.zookeepers}")
    Map(
      "user" -> this.user,
      "password" -> this.password,
      "instanceId" -> this.instance,
      "zookeepers" -> this.zookeepers,
      "tableName" -> gwNamespace
    )
  }
}
