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

import scala.concurrent.Future

object GeolifeQueries
    extends BaseService
    with CirceSupport
    with AkkaSystem.LoggerExecutor {

  val GM_SFT = "gmtrajectory"
  val GW_SFT = "gwtrajectory"

  // We query the spatio-temporally indexed version if gw3D is defined.
  def geowaveQuerier(gw3d: Boolean) =
    if(gw3d) {
      GeoWaveQuerier("geowave.geolife3Dp", GW_SFT + "3Dp")
    } else {
      GeoWaveQuerier("geowave.geolife", GW_SFT)
    }

  def geomesaFeatureSource() = {
    val geomesaDs = GeoMesaConnection.dataStore("geomesa.geolife")
    geomesaDs.getFeatureSource(GM_SFT)
  }

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

        def name(queryName: String, gw3d: Boolean) =
          if(gw3d) {
            queryName + "-GW3D"
          } else {
            queryName
          }

        pathPrefix("in-beijing-count") {
          // Query the multipolygon of the city of Beijing, and count results on the server side.

          val queryName = "GEOLIFE-IN-BEIJING-COUNT"

          pathEndOrSingleSlash {
            get {
              parameters('test ?, 'gw3d ?) { (isTestOpt, gw3d) =>
                val isTest = checkIfIsTest(isTestOpt)
                complete {
                  Future {
                    val mesa: TestResult = {
                      TestResult.capture(GeoMesaConnection.clusterId) { _ =>
                        val geomesaQuery = new Query(GM_SFT, ECQL.toFilter(Beijing.CQL.inBeijing))
                        geomesaQuery.getHints.put(org.locationtech.geomesa.accumulo.index.QueryHints.EXACT_COUNT,true)

                        geomesaFeatureSource().getCount(geomesaQuery)
                      }
                    }

                    val wave: TestResult =
                      TestResult.capture(GeoWaveConnection.clusterId, {
                        geowaveQuerier(gw3d.isDefined).spatialQueryCount(Beijing.geom.jtsGeom)
                      })

                    val result = RunResult(name(queryName, gw3d.isDefined), mesa, wave, isTest)
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
              parameters('test ?, 'gw3d ?) { (isTestOpt, gw3d) =>
                val isTest = checkIfIsTest(isTestOpt)
                complete {
                  Future {
                    val mesa: TestResult = {
                      TestResult.capture(GeoMesaConnection.clusterId, {
                        val geomesaQuery = new Query(GM_SFT, ECQL.toFilter(Beijing.CQL.inBeijing))

                        geomesaFeatureSource().getFeatures(geomesaQuery)
                      })
                    }

                    val wave: TestResult =
                      TestResult.capture(GeoWaveConnection.clusterId, {
                        geowaveQuerier(gw3d.isDefined).spatialQuery(Beijing.geom.jtsGeom)
                      })

                    val result = RunResult(name(queryName, gw3d.isDefined), mesa, wave, isTest)
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
              parameters('test ?, 'gw3d ?) { (isTestOpt, gw3d) =>
                val isTest = checkIfIsTest(isTestOpt)
                complete {
                  Future {
                    val mesa: TestResult = {
                      TestResult.capture(GeoMesaConnection.clusterId) { _ =>
                        val geomesaQuery = new Query(GM_SFT, ECQL.toFilter(Beijing.CQL.inBoundingBox))
                        geomesaQuery.getHints.put(org.locationtech.geomesa.accumulo.index.QueryHints.EXACT_COUNT,true)

                        geomesaFeatureSource().getCount(geomesaQuery)
                      }
                    }

                    val wave: TestResult =
                      TestResult.capture(GeoWaveConnection.clusterId, {
                        geowaveQuerier(gw3d.isDefined).spatialQueryCount(Beijing.boundingBoxGeom)
                      })

                    val result = RunResult(name(queryName, gw3d.isDefined), mesa, wave, isTest)
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
              parameters('test ?, 'gw3d ?) { (isTestOpt, gw3d) =>
                val isTest = checkIfIsTest(isTestOpt)
                complete {
                  Future {
                    val mesa: TestResult = {
                      TestResult.capture(GeoMesaConnection.clusterId, {
                        val geomesaQuery = new Query(GM_SFT, ECQL.toFilter(Beijing.CQL.inBoundingBox))

                        geomesaFeatureSource().getFeatures(geomesaQuery)
                      })
                    }

                    val wave: TestResult =
                      TestResult.capture(GeoWaveConnection.clusterId, {
                        geowaveQuerier(gw3d.isDefined).spatialQuery(Beijing.boundingBoxGeom)
                      })

                    val result = RunResult(name(queryName, gw3d.isDefined), mesa, wave, isTest)
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
              parameters('tile_width.as[Int], 'test ?, 'gw3d ?) { (tileWidth, isTestOpt, gw3d) =>
                val isTest = checkIfIsTest(isTestOpt)
                complete {
                  Future {
                    // Generate the bounding boxes
                    val bboxes = Beijing.boundingBoxes(tileWidth)

                    (for((col, row, bbox) <- bboxes) yield {
                      val jtsGeom = bbox.toPolygon.jtsGeom

                      val mesa: TestResult = {
                        TestResult.capture(GeoMesaConnection.clusterId, {
                          val geomesaQuery = new Query(GM_SFT, ECQL.toFilter(CQLUtils.toBBOXquery("the_geom", bbox)))

                          geomesaFeatureSource().getFeatures(geomesaQuery)
                        })
                      }

                      val wave: TestResult =
                        TestResult.capture(GeoWaveConnection.clusterId, {
                          geowaveQuerier(gw3d.isDefined).spatialQuery(jtsGeom)
                        })

                      val result = RunResult(name(queryName(tileWidth, col, row), gw3d.isDefined), mesa, wave, isTest)
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

                    val mesa: TestResult = {
                      TestResult.capture(GeoMesaConnection.clusterId) { _ =>
                        val geomesaQuery = new Query(GM_SFT, ECQL.toFilter(tq.toCQL("timestamp")))
                        geomesaQuery.getHints.put(org.locationtech.geomesa.accumulo.index.QueryHints.EXACT_COUNT,true)

                        geomesaFeatureSource().getCount(geomesaQuery)
                      }
                    }

                    val wave: TestResult =
                      TestResult.capture(GeoWaveConnection.clusterId, {
                        geowaveQuerier(true).temporalQueryCount(tq)
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

                    val mesa: TestResult = {
                      TestResult.capture(GeoMesaConnection.clusterId, {
                        val geomesaQuery = new Query(GM_SFT, ECQL.toFilter(tq.toCQL("timestamp")))
                        geomesaFeatureSource().getFeatures(geomesaQuery)
                      })
                    }

                    val wave: TestResult =
                      TestResult.capture(GeoWaveConnection.clusterId, {
                        geowaveQuerier(true).temporalQuery(tq)
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

                    val mesa: TestResult = {
                      TestResult.capture(GeoMesaConnection.clusterId) { _ =>
                        val geomesaQuery = new Query(GM_SFT, ECQL.toFilter(tq.toCQL("timestamp")))
                        geomesaQuery.getHints.put(org.locationtech.geomesa.accumulo.index.QueryHints.EXACT_COUNT,true)

                        geomesaFeatureSource().getCount(geomesaQuery)
                      }
                    }

                    val wave: TestResult =
                      TestResult.capture(GeoWaveConnection.clusterId, {
                        geowaveQuerier(true).temporalQueryCount(tq)
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

                    val mesa: TestResult = {
                      TestResult.capture(GeoMesaConnection.clusterId, {
                        val geomesaQuery = new Query(GM_SFT, ECQL.toFilter(tq.toCQL("timestamp")))
                        geomesaFeatureSource().getFeatures(geomesaQuery)
                      })
                    }

                    val wave: TestResult =
                      TestResult.capture(GeoWaveConnection.clusterId, {
                        geowaveQuerier(true).temporalQuery(tq)
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

                    val mesa: TestResult = {
                      TestResult.capture(GeoMesaConnection.clusterId, {
                        val geomesaQuery = new Query(GM_SFT, ECQL.toFilter(Beijing.CQL.inBeijing + " AND " + tq.toCQL("timestamp")))
                        geomesaFeatureSource().getFeatures(geomesaQuery)
                      })
                    }

                    val wave: TestResult =
                      TestResult.capture(GeoWaveConnection.clusterId, {
                        geowaveQuerier(true).spatialTemporalQuery(Beijing.geom.jtsGeom, tq)
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
      }
    }
}
