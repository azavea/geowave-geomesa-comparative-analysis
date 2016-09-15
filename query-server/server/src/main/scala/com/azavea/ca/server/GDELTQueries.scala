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

object GDELTQueries
    extends BaseService
    with CirceSupport
    with AkkaSystem.LoggerExecutor {

  val GM_SFT = "gdelt"
  val GW_SFT = "gdelt"

  // Only use for server-side count aggregations.
  def geowaveQuerier() =
    GeoWaveQuerier("geowave.gdelt", GW_SFT)

  lazy val geomesaDs = GeoMesaConnection.dataStore("geomesa.gdelt")
  lazy val geowaveDs = GeoWaveConnection.geotoolsDataStore("geowave.gdelt")

  def geowaveFeatureSource() = geowaveDs.getFeatureSource(GW_SFT)
  def geomesaFeatureSource() = geomesaDs.getFeatureSource(GM_SFT)

  def captureGeoWaveQuery(query: Filter): TestResult =
    TestResult.capture(GeoWaveConnection.clusterId, {
      geowaveFeatureSource().getFeatures(new Query(GW_SFT, query))
    })

  def captureGeoMesaQuery(query: Filter, loose: Boolean = false): TestResult = {
    val q = new Query(GM_SFT, query)
    q.getHints.put(org.locationtech.geomesa.accumulo.index.QueryHints.LOOSE_BBOX, loose)
    TestResult.capture(GeoMesaConnection.clusterId, {
      geomesaFeatureSource().getFeatures(q)
    })
  }

  def captureGeoMesaCountQuery(query: Filter, loose: Boolean = false): TestResult = {
    val q = new Query(GM_SFT, query)
    q.getHints.put(org.locationtech.geomesa.accumulo.index.QueryHints.EXACT_COUNT, true)
    q.getHints.put(org.locationtech.geomesa.accumulo.index.QueryHints.LOOSE_BBOX, loose)
    TestResult.capture(GeoMesaConnection.clusterId, {
      geomesaFeatureSource().getFeatures(q)
    })
  }

  def looseSuffix(opt: Option[String]): String =
    if(checkIfIsLoose(opt)) "-LOOSE"
    else ""

  def routes =
    pathPrefix("geolife") {
      pathPrefix("ping") {
        pathEndOrSingleSlash {
          get {
            complete { Future { "pong" } } }
        }
      } ~
      pathPrefix("spatial-only") {

        // Spatial Only queries.

        pathPrefix("in-beijing-count") {
          // Query the multipolygon of the city of Beijing, and count results on the server side.

          val queryName = "GEOLIFE-IN-BEIJING-COUNT"

          pathEndOrSingleSlash {
            get {
              parameters('test ?) { isTestOpt =>
                val isTest = checkIfIsTest(isTestOpt)
                complete {
                  Future {
                    val query = ECQL.toFilter(Beijing.CQL.inBeijing)

                    val mesa: TestResult = captureGeoMesaCountQuery(query)

                    val wave: TestResult =
                      TestResult.capture(GeoWaveConnection.clusterId, {
                        geowaveQuerier().spatialQueryCount(Beijing.geom.jtsGeom)
                      })

                    val result = RunResult(queryName, mesa, wave, isTest)
                    DynamoDB.saveResult(result)
                    result
                  }
                }
              }
            }
          }
        } ~
        pathPrefix("in-beijing-iterate") {
          // Query the multipolygon of the city of Beijing, and count the results by iterating over them on the client side.

          val queryName = "GEOLIFE-IN-BEIJING-ITERATE"

          pathEndOrSingleSlash {
            get {
              parameters('test ?) { isTestOpt =>
                val isTest = checkIfIsTest(isTestOpt)
                complete {
                  Future {
                    val query = ECQL.toFilter(Beijing.CQL.inBeijing)

                    val mesa: TestResult = captureGeoMesaQuery(query)
                    val wave: TestResult = captureGeoWaveQuery(query)

                    val result = RunResult(queryName, mesa, wave, isTest)
                    DynamoDB.saveResult(result)
                    result
                  }
                }
              }
            }
          }
        } ~
        pathPrefix("in-beijing-bbox-count") {
          // Query the bounding box of the city of Beijing, and count results on the server side.

          val queryName = "GEOLIFE-IN-BEIJING-BBOX-COUNT"

          pathEndOrSingleSlash {
            get {
              parameters('test ?, 'loose ?) { (isTestOpt, isLooseOpt) =>
                val isTest = checkIfIsTest(isTestOpt)
                complete {
                  Future {
                    val query = ECQL.toFilter(Beijing.CQL.inBoundingBox)

                    val mesa: TestResult = captureGeoMesaCountQuery(query, checkIfIsLoose(isLooseOpt))

                    val wave: TestResult =
                      TestResult.capture(GeoWaveConnection.clusterId, {
                        geowaveQuerier().spatialQueryCount(Beijing.boundingBoxGeom)
                      })

                    val result = RunResult(queryName + looseSuffix(isLooseOpt), mesa, wave, isTest)
                    DynamoDB.saveResult(result)
                    result
                  }
                }
              }
            }
          }
        } ~
        pathPrefix("in-beijing-bbox-iterate") {
          // Query the bounding box of the city of Beijing, and count the results by iterating over them on the client side.

          val queryName = "GEOLIFE-IN-BEIJING-BBOX-ITERATE"

          pathEndOrSingleSlash {
            get {
              parameters('test ?, 'loose ?) { (isTestOpt, isLooseOpt) =>
                val isTest = checkIfIsTest(isTestOpt)
                complete {
                  Future {
                    val query = ECQL.toFilter(Beijing.CQL.inBoundingBox)

                    val mesa: TestResult = captureGeoMesaQuery(query, checkIfIsLoose(isLooseOpt))
                    val wave: TestResult = captureGeoWaveQuery(query)

                    val result = RunResult(queryName + looseSuffix(isLooseOpt), mesa, wave, isTest)
                    DynamoDB.saveResult(result)
                    result
                  }
                }
              }
            }
          }
        } ~
        pathPrefix("beijing-bboxes-iterate") {
          // Query portions of the bounding box of the city of Beijing, and count the results by iterating over them on the client side.

          def queryName(tileWidth: Int, col: Int, row: Int): String = s"GEOLIFE-BEIJING-BBOXES-ITERATE-${tileWidth}-${col}-${row}"

          pathEndOrSingleSlash {
            get {
              parameters('tile_width.as[Int], 'test ?, 'loose ?) { (tileWidth, isTestOpt, isLooseOpt) =>
                val isTest = checkIfIsTest(isTestOpt)
                complete {
                  Future {
                    // Generate the bounding boxes
                    val bboxes = Beijing.boundingBoxes(tileWidth)

                    (for((col, row, bbox) <- bboxes) yield {
                      val query = ECQL.toFilter(CQLUtils.toBBOXquery("the_geom", bbox))

                      val mesa: TestResult = captureGeoMesaQuery(query, checkIfIsLoose(isLooseOpt))
                      val wave: TestResult = captureGeoWaveQuery(query)

                      val result = RunResult(queryName(tileWidth, col, row) + looseSuffix(isLooseOpt), mesa, wave, isTest)
                      DynamoDB.saveResult(result)
                      result
                    }).toArray
                  }
                }
              }
            }
          }
        }
      } ~
      pathPrefix("temporal-only") {

        // Temporal Only queries.

        pathPrefix("in-2011-count") {
          // Query the dataset and return everything in 2011

          val queryName = "GEOLIFE-IN-2011-COUNT"

          pathEndOrSingleSlash {
            get {
              parameters('test ?) { isTestOpt =>
                val isTest = checkIfIsTest(isTestOpt)
                complete {
                  Future {
                    val tq = TimeQuery("2011-01-01T00:00:00", "2012-01-01T00:00:00")
                    val query = ECQL.toFilter(tq.toCQL("timestamp"))

                    val mesa: TestResult = captureGeoMesaCountQuery(query)
                    val wave: TestResult =
                      TestResult.capture(GeoWaveConnection.clusterId, {
                        geowaveQuerier().temporalQueryCount(tq, pointOnly = true)
                      })

                    val result = RunResult(queryName, mesa, wave, isTest)
                    DynamoDB.saveResult(result)
                    result
                  }
                }
              }
            }
          }
        } ~
        pathPrefix("in-2011-iterate") {
          // Query the dataset and return everything in 2011

          val queryName = "GEOLIFE-IN-2011-ITERATE"

          pathEndOrSingleSlash {
            get {
              parameters('test ?) { isTestOpt =>
                val isTest = checkIfIsTest(isTestOpt)
                complete {
                  Future {
                    val tq = TimeQuery("2011-01-01T00:00:00", "2012-01-01T00:00:00")
                    val query = ECQL.toFilter(tq.toCQL("timestamp"))

                    val mesa: TestResult = captureGeoMesaQuery(query)
                    val wave: TestResult =
                      TestResult.capture(GeoWaveConnection.clusterId, {
                        geowaveQuerier().temporalQuery(tq, pointOnly = true)
                      })

                    val result = RunResult(queryName, mesa, wave, isTest)
                    DynamoDB.saveResult(result)
                    result
                  }
                }
              }
            }
          }
        } ~
        pathPrefix("in-aug-2011-count") {
          // Query the dataset and return everything in August 2011

          val queryName = "GEOLIFE-IN-AUG-2011-COUNT"

          pathEndOrSingleSlash {
            get {
              parameters('test ?) { isTestOpt =>
                val isTest = checkIfIsTest(isTestOpt)
                complete {
                  Future {
                    val tq = TimeQuery("2011-08-01T00:00:00", "2011-09-01T00:00:00")
                    val query = ECQL.toFilter(tq.toCQL("timestamp"))

                    val mesa: TestResult = captureGeoMesaCountQuery(query)
                    val wave: TestResult =
                      TestResult.capture(GeoWaveConnection.clusterId, {
                        geowaveQuerier().temporalQueryCount(tq, pointOnly = true)
                      })

                    val result = RunResult(queryName, mesa, wave, isTest)
                    DynamoDB.saveResult(result)
                    result
                  }
                }
              }
            }
          }
        } ~
        pathPrefix("in-aug-2011-iterate") {
          // Query the dataset and return everything in August 2011

          val queryName = "GEOLIFE-IN-2011-ITERATE"

          pathEndOrSingleSlash {
            get {
              parameters('test ?) { isTestOpt =>
                val isTest = checkIfIsTest(isTestOpt)
                complete {
                  Future {
                    val tq = TimeQuery("2011-08-01T00:00:00", "2011-09-01T00:00:00")
                    val query = ECQL.toFilter(tq.toCQL("timestamp"))

                    val mesa: TestResult = captureGeoMesaQuery(query)
                    val wave: TestResult =
                      TestResult.capture(GeoWaveConnection.clusterId, {
                        geowaveQuerier().temporalQuery(tq, pointOnly = true)
                      })

                    val result = RunResult(queryName, mesa, wave, isTest)
                    DynamoDB.saveResult(result)
                    result
                  }
                }
              }
            }
          }
        }
      } ~
      pathPrefix("spatiotemporal") {
        // Temporal Only queries.

        pathPrefix("in-beijing-aug-2011-iterate") {
          // Query the dataset and return everything in Beijing in August 2011

          val queryName = "GEOLIFE-IN-BEIJING-AUG-2011-ITERATE"

          pathEndOrSingleSlash {
            get {
              parameters('test ?) { isTestOpt =>
                val isTest = checkIfIsTest(isTestOpt)
                complete {
                  Future {
                    val tq = TimeQuery("2011-08-01T00:00:00", "2011-09-01T00:00:00")
                    val query = ECQL.toFilter(Beijing.CQL.inBeijing + " AND " + tq.toCQL("timestamp"))

                    val mesa: TestResult = captureGeoMesaQuery(query)
                    val wave: TestResult = captureGeoWaveQuery(query)

                    val result = RunResult(queryName, mesa, wave, isTest)
                    DynamoDB.saveResult(result)
                    result
                  }
                }
              }
            }
          }
        } ~
        pathPrefix("in-beijing-aug-2011-count") {
          // Query the dataset and return everything in Beijing in August 2011

          val queryName = "GEOLIFE-IN-BEIJING-AUG-2011-COUNT"

          pathEndOrSingleSlash {
            get {
              parameters('test ?) { isTestOpt =>
                val isTest = checkIfIsTest(isTestOpt)
                complete {
                  Future {
                    val tq = TimeQuery("2011-08-01T00:00:00", "2011-09-01T00:00:00")
                    val query = ECQL.toFilter(Beijing.CQL.inBeijing + " AND " + tq.toCQL("timestamp"))

                    val mesa: TestResult = captureGeoMesaCountQuery(query)
                    val wave: TestResult =
                      TestResult.capture(GeoWaveConnection.clusterId, {
                        geowaveQuerier().spatialTemporalQueryCount(Beijing.geom.jtsGeom, tq, pointOnly = true)
                      })

                    val result = RunResult(queryName, mesa, wave, isTest)
                    DynamoDB.saveResult(result)
                    result
                  }
                }
              }
            }
          }
        } ~
        pathPrefix("in-center-beijing-jan-2011-iterate") {
          val queryName = "GEOLIFE-IN-CENTER-BEIJING-JAN-2011-ITERATE"

          pathEndOrSingleSlash {
            get {
              parameters('test ?) { isTestOpt =>
                val isTest = checkIfIsTest(isTestOpt)
                complete {
                  Future {
                    val tq = TimeQuery("2011-01-01T00:00:00", "2011-02-01T00:00:00")
                    val query = ECQL.toFilter(Beijing.CQL.inBeijingCenter + " AND " + tq.toCQL("timestamp"))

                    val mesa: TestResult = captureGeoMesaQuery(query)
                    val wave: TestResult = captureGeoWaveQuery(query)

                    val result = RunResult(queryName, mesa, wave, isTest)
                    DynamoDB.saveResult(result)
                    result
                  }
                }
              }
            }
          }
        } ~
        pathPrefix("in-center-beijing-jan-2011-count") {
          val queryName = "GEOLIFE-IN-CENTER-BEIJING-JAN-2011-COUNT"

          pathEndOrSingleSlash {
            get {
              parameters('test ?) { isTestOpt =>
                val isTest = checkIfIsTest(isTestOpt)
                complete {
                  Future {
                    val tq = TimeQuery("2011-01-01T00:00:00", "2011-02-01T00:00:00")
                    val query = ECQL.toFilter(Beijing.CQL.inBeijingCenter + " AND " + tq.toCQL("timestamp"))

                    val mesa: TestResult = captureGeoMesaCountQuery(query)
                    val wave: TestResult =
                      TestResult.capture(GeoWaveConnection.clusterId, {
                        geowaveQuerier().spatialTemporalQueryCount(Beijing.centerGeom.jtsGeom, tq, pointOnly = true)
                      })

                    val result = RunResult(queryName, mesa, wave, isTest)
                    DynamoDB.saveResult(result)
                    result
                  }
                }
              }
            }
          }
        } ~
        pathPrefix("in-center-beijing-bbox-feb-2011-iterate") {
          val queryName = "GEOLIFE-IN-CENTER-BEIJING-BBOX-FEB-2011-ITERATE"

          pathEndOrSingleSlash {
            get {
              parameters('test ?, 'loose ?) { (isTestOpt, isLooseOpt) =>
                val isTest = checkIfIsTest(isTestOpt)
                complete {
                  Future {
                    val tq = TimeQuery("2011-02-01T00:00:00", "2011-03-01T00:00:00")
                    val query = ECQL.toFilter(Beijing.CQL.inBeijingCenterBBOX + " AND " + tq.toCQL("timestamp"))

                    val mesa: TestResult = captureGeoMesaQuery(query, checkIfIsLoose(isLooseOpt))
                    val wave: TestResult = captureGeoWaveQuery(query)

                    val result = RunResult(queryName + looseSuffix(isLooseOpt), mesa, wave, isTest)
                    DynamoDB.saveResult(result)
                    result
                  }
                }
              }
            }
          }
        } ~
        pathPrefix("in-center-beijing-bbox-feb-2011-count") {
          val queryName = "GEOLIFE-IN-CENTER-BEIJING-BBOX-FEB-2011-COUNT"

          pathEndOrSingleSlash {
            get {
              parameters('test ?, 'loose ?) { (isTestOpt, isLooseOpt) =>
                val isTest = checkIfIsTest(isTestOpt)
                complete {
                  Future {
                    val tq = TimeQuery("2011-02-01T00:00:00", "2011-03-01T00:00:00")
                    val query = ECQL.toFilter(Beijing.CQL.inBeijingCenterBBOX + " AND " + tq.toCQL("timestamp"))

                    val mesa: TestResult = captureGeoMesaCountQuery(query, checkIfIsLoose(isLooseOpt))
                    val wave: TestResult =
                      TestResult.capture(GeoWaveConnection.clusterId, {
                        geowaveQuerier().spatialTemporalQueryCount(Beijing.centerGeom.envelope.toPolygon.jtsGeom, tq, pointOnly = true)
                      })

                    val result = RunResult(queryName + looseSuffix(isLooseOpt), mesa, wave, isTest)
                    DynamoDB.saveResult(result)
                    result
                  }
                }
              }
            }
          }
        }
      }
    }
}
