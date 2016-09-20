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
import java.time._
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

  def capture(isLooseOpt: Option[String], waveOrMesa: String, cql: String): (Option[TestResult], Option[TestResult]) = {
    val query = ECQL.toFilter(cql)
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
  }

  def queryRoute(queryName: String, cql: String) = get {
    pathEndOrSingleSlash {
      parameters('test ?, 'loose ?, 'waveOrMesa ? "wm") { (isTestOpt, isLooseOpt, waveOrMesa) =>
        val isTest = checkIfIsTest(isTestOpt)
        complete {
          Future {
            val (mesa, wave) = capture(isLooseOpt, waveOrMesa, cql)
            val result = RunResult(s"${queryName}${looseSuffix(isLooseOpt)}", mesa, wave, isTest)
            DynamoDB.saveResult(result)
          }
        }
      }
    }
  }

  def queryGridRoute(queryPrefix: String, period: Period) =
    path(IntNumber / IntNumber / IntNumber / IntNumber) { (timeIndex, z, col, row) =>
      val queryName = s"$queryPrefix-$timeIndex-$z-$col-$row"
      parameters('test ?, 'loose ?, 'waveOrMesa ? "wm") { (isTestOpt, isLooseOpt, waveOrMesa) =>
        complete {
          Future {
            val isTest = checkIfIsTest(isTestOpt)
            val extent = Tracks.gridUSA(z)(col, row)
            val startDate = Tracks.firstDate.plus(period.multipliedBy(timeIndex))
            val endDate = Tracks.firstDate.plus(period.multipliedBy(timeIndex+1))
            val cql = Tracks.CQL.forExtent(extent) + " AND " + Tracks.CQL.forTime(startDate, endDate)
            val (mesa, wave) = capture(isLooseOpt, waveOrMesa, cql)
            val result = RunResult(s"${queryName}${looseSuffix(isLooseOpt)}", mesa, wave, isTest)
            DynamoDB.saveResult(result)
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
              parameters('test ?, 'loose ? , 'wm ? "wm") { (isTestOpt, isLooseOpt, waveOrMesa) =>
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
                       val cql = Tracks.CQL.inCA + " AND " + tq.toCQL("TimeStamp")
                       val (mesa, wave) = capture(isLooseOpt, waveOrMesa, cql)

                       val result = RunResult(s"${queryName}-${suffix}${looseSuffix(isLooseOpt)}", mesa, wave, isTest)
                       DynamoDB.saveResult(result)
                     }).toArray
                  }
                }
              }
            }
          }
        } ~
        pathPrefix("grid-query" / Segment) { testContext =>
          pathPrefix("5-day") {
            queryGridRoute(s"TRACKS-USA-GRID-${testContext.toUpperCase}-5DAY", Period.ofDays(5))
          } ~
          pathPrefix("1-week") {
            queryGridRoute(s"TRACKS-USA-GRID--${testContext.toUpperCase}-1WEEK", Period.ofWeeks(1))
          } ~
          pathPrefix("9-day") {
            queryGridRoute(s"TRACKS-USA-GRID-${testContext.toUpperCase}-9DAY", Period.ofDays(9))
          } ~
          pathPrefix("17-day") {
            queryGridRoute(s"TRACKS-USA-GRID-${testContext.toUpperCase}-18DAY", Period.ofDays(18))
          } ~
          pathPrefix("27-day") {
            queryGridRoute(s"TRACKS-USA-GRID-${testContext.toUpperCase}-27DAY", Period.ofDays(27))
          } ~
          pathPrefix("1-month") {
            queryGridRoute(s"TRACKS-USA-GRID-${testContext.toUpperCase}-1MONTH", Period.ofMonths(1))
          }
        }
    }
}
