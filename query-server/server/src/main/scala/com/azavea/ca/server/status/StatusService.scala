package com.azavea.ca.server.status

import java.lang.management.ManagementFactory
import akka.http.scaladsl.server.Directives._
import io.circe.generic.auto._
import org.geotools.data.DataStoreFinder
import org.locationtech.geomesa.accumulo.data.AccumuloDataStore
import org.opengis.filter.Filter

import scala.concurrent.duration._

import com.azavea.ca.server._
import com.azavea.ca.server.geomesa.connection._
import com.azavea.ca.server.geowave.connection._

object StatusService extends BaseService with AkkaSystem.LoggerExecutor {

  /** This endpoint will return server uptime */
  def system = {
    log.info("/status/uptime executed")
    complete(Status(Duration(ManagementFactory.getRuntimeMXBean.getUptime, MILLISECONDS).toString()))
  }

  /** This endpoint will return a count of all simple feature types found on a given catalog */
  def geomesa(tableName: String) = complete {
    log.info(s"/geomesa/${tableName}/status executed")
    val ds = GeoMesaConnection.dataStore(tableName)
    val typeNames = ds.getTypeNames
    val countsMap: Map[String, String] = typeNames.map { typeName =>
      val sft = ds.getSchema(typeName)
      typeName -> ds.stats.getCount(sft, Filter.INCLUDE, false).map(_.toString).getOrElse("Unknown")
    }.toMap
    countsMap
  }

  /** This endpoint will return a count of all simple feature types found on a given catalog */
  def geowave(tableName: String) = complete {
    val gwBasicOperations = GeoWaveConnection.basicOperations(tableName)
    log.info(s"/geowave/${tableName}/status executed")
    gwBasicOperations.getRowCount(tableName)
  }
}
