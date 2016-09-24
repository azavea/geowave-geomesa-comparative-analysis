package com.azavea.ca.server

import com.azavea.ca.core._
import com.azavea.ca.server.results._
import com.azavea.ca.server.geomesa.connection.GeoMesaConnection
import com.azavea.ca.server.geowave.connection.GeoWaveConnection
import com.azavea.ca.server.geowave.GeoWaveQuerier

import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce._
import io.circe.generic.auto._
import geotrellis.vector._
import org.geotools.data._
import org.geotools.filter.text.ecql.ECQL
import org.opengis.filter.Filter

import scala.concurrent.Future

object TranslatedViennaQueries
    extends BaseService
    with CAQueryUtils
    with CirceSupport
    with AkkaSystem.LoggerExecutor {

  val gwTableName = "geowave.transvienna"
  val gwFeatureTypeName = "vienna-buildings"

  val gmTableName = "geomesa.transvienna"
  val gmFeatureTypeName = "vienna-buildings"

  def routes =
    pathPrefix("transvienna") {
      pathPrefix("ping") {
        pathEndOrSingleSlash {
          get {
            complete { Future { "pong" } } }
        }
      } ~
      pathPrefix("reset") {
        pathEndOrSingleSlash {
          get {
            complete { Future { resetDataStores() ; "done" } } }
        }
      } ~
      pathPrefix("spatial") {
        pathPrefix("translated-vienna-buffers") {
          val queryName = "TRANS-VIENNA-BUFFERS"

          pathEndOrSingleSlash {
            get {
              parameters("lng".as[Double], "lat".as[Double], 'test ?) { (lng, lat, isTestOpt) =>
                val isTest = checkIfIsTest(isTestOpt)
                complete {
                  Future {
                    val point = Point(lng, lat)
                    val buffers: Seq[(String, Polygon)] = Seq(1, 2, 3, 5, 8, 13, 20).map { z =>
                      val m = z * 10000
                      (s"${z*10}-KM", Cities.buffer(point, m))
                    }

                    (for((size, geom) <- buffers) yield {
                      val suffix = s"($lng,$lat)-$size"
                      val query = ECQL.toFilter(CQLUtils.intersects("the_geom", geom))

                      val mesa: TestResult = captureGeoMesaQuery(query)
                      val wave: TestResult = captureGeoWaveQuery(query)

                      val result = RunResult(s"${queryName}-${suffix}", mesa, wave, isTest)
                      DynamoDB.saveResult(result)
                      result
                    }).toArray

                  }
                }
              }
            }
          }
        }
      }
    }
}
