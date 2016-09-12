package com.azavea.ca.server.geomesa.connection

import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus
import net.ceedubs.ficus.readers.ArbitraryTypeReader
import org.geotools.data.DataStoreFinder
import org.locationtech.geomesa.accumulo.data.AccumuloDataStore

case class GeoMesaConnectionConfig(
  user: String,
  password: String,
  instance: String,
  zookeepers: String,
  cluster: String
) {
  def toParamsMap(tableName: String) = {
    println(s"Creating GeoMesa connection with: table->${tableName}, user->${this.user}, password->${this.password}, instance->${this.instance}, and zk->${this.zookeepers}")
    Map(
      "user" -> this.user,
      "password" -> this.password,
      "instanceId" -> this.instance,
      "zookeepers" -> this.zookeepers,
      "tableName" -> tableName
    )
  }
}
