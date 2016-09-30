package com.azavea.ca.server

import com.azavea.ca.server.geomesa.connection._
import com.azavea.ca.server.geowave.connection._
import com.azavea.ca.server.status._

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import scala.concurrent.duration._
import scala.collection.concurrent.TrieMap

object Routes {

  lazy val randomExtentQueryRoutes =
    withRequestTimeout(60.seconds) {
      RandomExtentQuery.queryBoth(geowave.GeoWaveRandomExtentQuery.query, geomesa.GeoMesaRandomExtentQuery.query)
    }

  lazy val systemRoutes =
    pathPrefix("system") {
      pathPrefix("status") {
        pathEndOrSingleSlash {
          get {
            StatusService.system
          }
        }
      }
    }

  lazy val geomesaRoutes =
    pathPrefix("geomesa") {
      pathPrefix(Segment) { tableName =>
        pathPrefix("status") {
          get {
            StatusService.geomesa(tableName)
          }
        } ~
        pathPrefix("sfts") {
          pathEndOrSingleSlash {
            get {
              geomesa.SimpleFeatureTypes.list(tableName)
            }
          } ~
          pathPrefix(Segment) { sftName =>
            get {
              geomesa.SimpleFeatureTypes.detail(tableName, sftName)
            }
          }
        }
      }
    }

  lazy val geowaveRoutes =
    pathPrefix("geowave") {
      pathPrefix(Segment) { tableName =>
        pathPrefix("status") {
          get {
            StatusService.geowave(tableName)
          }
        } ~
        pathPrefix("sfts") {
          pathEndOrSingleSlash {
            get {
              geowave.SimpleFeatureTypes.list(tableName)
            }
          } ~
          pathPrefix(Segment) { sftName =>
            get {
              geowave.SimpleFeatureTypes.detail(tableName, sftName)
            }
          }
        }
      }
    }

  def apply() =
    systemRoutes ~
      geomesaRoutes ~
      geowaveRoutes ~
      GeolifeQueries.routes ~
      GDELTQueries.routes ~
      TranslatedViennaQueries.routes ~
      TracksQueries.routes ~
      CitiesQueries.routes ~
      randomExtentQueryRoutes
}
