package com.azavea.ca.server.geomesa

import com.azavea.ca.server._
import com.azavea.ca.server.geomesa.connection._

import org.geotools.data.DataStoreFinder
import org.locationtech.geomesa.accumulo.data.AccumuloDataStore

import akka.http.scaladsl.server.Directives._
import io.circe.generic.auto._

import scala.collection.JavaConversions._


object SimpleFeatureTypes
    extends BaseService
    with AkkaSystem.LoggerExecutor {

  def list(tableName: String) = complete {
    val ds = GeoMesaConnection.dataStore(tableName)
    ds.getTypeNames().toList
  }

  def detail(tableName: String, typeName: String) = complete {
    val ds = GeoMesaConnection.dataStore(tableName)
    ds.getSchema(typeName)
      .getAttributeDescriptors
      .map { attr => attr.getLocalName() }
  }
}
