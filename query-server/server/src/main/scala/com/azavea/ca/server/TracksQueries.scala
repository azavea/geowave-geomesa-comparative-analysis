package com.azavea.ca.server

import com.azavea.ca.core._
import com.azavea.ca.server.results._
import com.azavea.ca.server.geomesa.connection.GeoMesaConnection
import com.azavea.ca.server.geowave.connection.GeoWaveConnection
import com.azavea.ca.server.geowave.GeoWaveQuerier

import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce._
import io.circe.generic.auto._
import org.geotools.data._
import org.geotools.filter.text.ecql.ECQL
import org.opengis.filter.Filter

import scala.concurrent.Future

object TracksQueries
    extends BaseService
    with CAQueryUtils
    with CirceSupport
    with AkkaSystem.LoggerExecutor {

  val gwTableName = "geowave.tracks"
  val gwFeatureTypeName = "generated-tracks"

  val gmTableName = "geomesa.tracks"
  val gmFeatureTypeName = "generated-tracks"

  def queryRoute(queryName: String, cql: String) = get {
    pathEndOrSingleSlash {
      parameters('test ?, 'loose ?, 'wm ? "both") { (isTestOpt, isLooseOpt, waveOrMesa) =>
        val isTest = checkIfIsTest(isTestOpt)
        complete {
          Future {
            val query = ECQL.toFilter(cql)
            val (mesa, wave) =
              if(waveOrMesa == "wm") {
                val mesa: TestResult = captureGeoMesaQuery(query, checkIfIsLoose(isLooseOpt))
                val wave: TestResult = captureGeoWaveQuery(query)
                (Some(mesa), Some(wave))
              } else if (waveOrMesa == "w") {
                val wave: TestResult = captureGeoWaveQuery(query)
                (None, Some(wave))
              } else {
                val mesa: TestResult = captureGeoMesaQuery(query, checkIfIsLoose(isLooseOpt))
                (Some(mesa), None)
              }

            val result = RunResult(s"${queryName}${looseSuffix(isLooseOpt)}", mesa, wave, isTest)
            DynamoDB.saveResult(result)
          }
        }
      }
    }
  }

  def routes =
    pathPrefix("tracks") {
      pathPrefix("ping") {
        pathEndOrSingleSlash {
          get {
            complete { Future { "pong?" } } }
        }
      } ~
      pathPrefix("reset") {
        pathEndOrSingleSlash {
          get {
            complete { Future { resetDataStores() ; "done" } } }
        }
      } ~
      pathPrefix("spatial-only") {
        pathPrefix("in-california") {
          queryRoute("TRACKS-IN-CA", Tracks.CQL.inCA)
        } ~
         pathPrefix("in-california-bbox") {
            queryRoute("TRACKS-IN-CA-BBOX", Tracks.CQL.inBoundingBoxCA)
          } ~
          pathPrefix("in-california-7-days") {
            queryRoute("TRACKS-IN-CA-7-DAY",
                       Tracks.CQL.inCA + " AND " +
                         TimeQuery("2015-01-01T00:00:00", "2015-01-07T00:00:00").toCQL("TimeStamp"))
          } ~
          pathPrefix("in-california-bbox-7-days") {
            queryRoute("TRACKS-IN-CA-BBOX-7-DAY",
                       Tracks.CQL.inCA + " AND " +
                         TimeQuery("2015-01-01T00:00:00", "2015-01-07T00:00:00").toCQL("TimeStamp"))
          } ~
          pathPrefix("in-california-1-month") {
            queryRoute("TRACKS-IN-CA-1-month",
                       Tracks.CQL.inCA + " AND " +
                         TimeQuery("2015-01-01T00:00:00", "2015-02-01T00:00:00").toCQL("TimeStamp"))
          } ~
          pathPrefix("in-california-bbox-1-month") {
            queryRoute("TRACKS-IN-CA-BBOX-1-MONTH",
                       Tracks.CQL.inCA + " AND " +
                         TimeQuery("2015-01-01T00:00:00", "2015-02-01T00:00:00").toCQL("TimeStamp"))
          } ~
          pathPrefix("in-california-months") {
            val queryName = "TRACKS-IN-CALIFORNIA-MONTHS"

            pathEndOrSingleSlash {
              get {
                parameters('test ?, 'loose ?) { (isTestOpt, isLooseOpt) =>
                  val isTest = checkIfIsTest(isTestOpt)
                  complete {
                    Future {
                      val timeQueries =
                        Seq(
                          (s"2015-JAN", TimeQuery(s"2015-01-01T00:00:00", s"2015-02-01T00:00:00")),
                          (s"2015-FEB", TimeQuery(s"2015-02-01T00:00:00", s"2015-03-01T00:00:00")),
                          (s"2015-MAR", TimeQuery(s"2015-03-01T00:00:00", s"2015-04-01T00:00:00")),
                          (s"2015-APR", TimeQuery(s"2015-04-01T00:00:00", s"2015-05-01T00:00:00")),
                          (s"2015-MAY", TimeQuery(s"2015-05-01T00:00:00", s"2015-06-01T00:00:00")),
                          (s"2015-JUN", TimeQuery(s"2015-06-01T00:00:00", s"2015-07-01T00:00:00")),
                          (s"2015-JUL", TimeQuery(s"2015-07-01T00:00:00", s"2015-08-01T00:00:00")),
                          (s"2015-AUG", TimeQuery(s"2015-08-01T00:00:00", s"2015-09-01T00:00:00")),
                          (s"2015-SEP", TimeQuery(s"2015-09-01T00:00:00", s"2015-10-01T00:00:00")),
                          (s"2015-OCT", TimeQuery(s"2015-10-01T00:00:00", s"2015-11-01T00:00:00")),
                          (s"2015-NOV", TimeQuery(s"2015-11-01T00:00:00", s"2015-12-01T00:00:00")),
                          (s"2015-DEC", TimeQuery(s"2015-12-01T00:00:00", s"2016-01-01T00:00:00"))
                        )

                      (for((suffix, tq) <- timeQueries) yield {
                         val query = ECQL.toFilter(Tracks.CQL.inCA + " AND " + tq.toCQL("TimeStamp"))

                         val mesa: TestResult = captureGeoMesaQuery(query, checkIfIsLoose(isLooseOpt))
                         val wave: TestResult = captureGeoWaveQuery(query)

                         val result = RunResult(s"${queryName}-${suffix}${looseSuffix(isLooseOpt)}", mesa, wave, isTest)
                         DynamoDB.saveResult(result)
                         result
                       }).toArray
                    }
                  }
                }
              }
            }
          }
        // TODO: multi-month query, what does it test?
        // - saves a result per month
      }
    }
}
