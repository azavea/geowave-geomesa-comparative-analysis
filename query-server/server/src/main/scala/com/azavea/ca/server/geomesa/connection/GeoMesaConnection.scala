package com.azavea.ca.server.geomesa.connection

import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus
import net.ceedubs.ficus.readers.ArbitraryTypeReader

import org.geotools.data.DataStoreFinder
import org.locationtech.geomesa.accumulo.data.AccumuloDataStore

object GeoMesaConnection {
  import Ficus._
  import ArbitraryTypeReader._
  import scala.collection.JavaConversions._

  private val config = ConfigFactory.load()
  protected lazy val geomesaConfig = config.as[GeoMesaConnectionConfig]("geomesa")

  lazy val clusterId = geomesaConfig.cluster

  def dataStore(tableName: String): AccumuloDataStore = {
    val dsParams = geomesaConfig.toParamsMap(tableName)

    DataStoreFinder
      .getDataStore(dsParams)
      .asInstanceOf[AccumuloDataStore]
  }
}
