package com.azavea.ca.server.geowave

import com.azavea.ca.server._
import com.azavea.ca.server.geowave.connection._

import mil.nga.giat.geowave.adapter.vector.FeatureDataAdapter
import org.opengis.feature.simple._

import akka.http.scaladsl.server.Directives._
import io.circe.generic.auto._


object SimpleFeatureTypes extends BaseService with AkkaSystem.LoggerExecutor {
  import scala.collection.JavaConversions._

  def list(tableName: String) = complete {
    val gwAdapterStore = GeoWaveConnection.adapterStore(tableName)
    log.info("/geowave/sfts executed")
    gwAdapterStore
      .getAdapters()
      .toList.map {
        _.asInstanceOf[FeatureDataAdapter].getType.getTypeName
      }
  }

  def detail(tableName: String, typeName: String) = ???
}
