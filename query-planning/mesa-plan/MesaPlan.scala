package com.azavea.ca.planning

import org.apache.accumulo.core.client.mock.MockInstance
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.apache.accumulo.core.security.Authorizations
import org.geotools.data.{ DataStoreFinder, Query }
import org.geotools.filter.text.cql2.CQL
import org.locationtech.geomesa.accumulo.data.AccumuloDataStore
import org.locationtech.geomesa.accumulo.index._
import org.locationtech.geomesa.accumulo.index.Strategy.StrategyType
import org.locationtech.geomesa.accumulo.index.Strategy.StrategyType.StrategyType
import org.locationtech.geomesa.utils.geotools.RichSimpleFeatureType.RichSimpleFeatureType
import org.locationtech.geomesa.utils.geotools.SimpleFeatureTypes

import com.vividsolutions.jts.geom._
import org.geotools.feature.simple.SimpleFeatureTypeBuilder
import org.opengis.feature.simple.SimpleFeatureType
import org.geotools.referencing.CRS

import scala.collection.JavaConverters._


// docker run -it --rm -p 50095:50095 --net=geowave --hostname leader --name leader jamesmcclain/geomesa:1.2.6
// docker run -it --rm --net=geowave -v $SPARK_HOME:/spark:ro -v $(pwd)/mesa-plan/target/scala-2.11:/jars:ro openjdk:8-jdk
object MesaPlan {

  def main(args: Array[String]): Unit = {

    // Create SimpleFeatureType
    val authorityFactory = CRS.getAuthorityFactory(true)
    val epsg4326 = authorityFactory.createCoordinateReferenceSystem("EPSG:4326")
    val sftb = (new SimpleFeatureTypeBuilder).minOccurs(1).maxOccurs(1).nillable(false)
    val sftName = "CommonSimpleFeatureType"
    sftb.setName(sftName)
    sftb.add("when", classOf[java.util.Date])
    sftb.setCRS(epsg4326)
    sftb.add("where", classOf[Point])
    val sft = sftb.buildFeatureType
    sft.getUserData.put(Constants.SF_PROPERTY_START_TIME, "when")

    // Create DataStore
    val dsConf = new java.util.HashMap[String, String]()
    dsConf.put("instanceId", args(0))
    dsConf.put("zookeepers", args(1))
    dsConf.put("user", args(2))
    dsConf.put("password", args(3))
    dsConf.put("tableName", args(4))
    val ds = DataStoreFinder.getDataStore(dsConf).asInstanceOf[AccumuloDataStore]
    ds.createSchema(sft)

    val n = 250
    val rng = new scala.util.Random

    (-30 to -5).foreach({ bits =>
      val times = (0 to n).map({ _ =>
        val x1 = (rng.nextDouble * 355) - 180.0
        val y1 = (rng.nextDouble * 175) - 90.0
        val x2 = x1 + 180*math.pow(2,bits)
        val y2 = y1 + 180*math.pow(2,bits)

        val filter = CQL.toFilter(s"BBOX(where, $x1, $y1, $x2, $y2) AND (when DURING 1970-01-03T00:00:00.000Z/1970-01-04T00:00:00.000Z)")
        val filterQuery = QueryFilter(StrategyType.Z3, Some(filter))
        val strategy = new Z3IdxStrategy(filterQuery)

        val qp = QueryPlanner(sft, ds)
        val _query = new Query(sftName, filter)
        QueryPlanner.configureQuery(_query, sft)
        val query = QueryPlanner.updateFilter(_query, sft)

        val hints = query.getHints
        val output = ExplainNull

        val before = System.currentTimeMillis
        val ranges = strategy.getQueryPlan(qp, hints, output)
        val after = System.currentTimeMillis

        after - before
      })

      println(times.sum / n.toDouble)
    })
  }
}
