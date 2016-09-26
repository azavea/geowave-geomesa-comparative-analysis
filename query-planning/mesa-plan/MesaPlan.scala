package com.azavea.ca.planning

import org.apache.accumulo.core.client.mock.MockInstance
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.apache.accumulo.core.security.Authorizations
import org.geotools.data.{ DataStoreFinder, Query }
import org.geotools.filter.text.ecql.ECQL
import org.locationtech.geomesa.accumulo.data.AccumuloDataStore
import org.locationtech.geomesa.accumulo.index._
import org.locationtech.geomesa.accumulo.index.Strategy.StrategyType
import org.locationtech.geomesa.accumulo.index.Strategy.StrategyType.StrategyType
import org.locationtech.geomesa.utils.geotools.RichSimpleFeatureType.RichSimpleFeatureType
import org.locationtech.geomesa.utils.geotools.SimpleFeatureTypes
import scala.collection.JavaConverters._


object MesaPlan {

  def main(args: Array[String]): Unit = {

    /********************************************************
     * Begin section copied from QueryPlannerTest.scala and *
     * TestWithDataStore.scala in the GeoMesa 1.2.6 tree.   *
     ********************************************************/

    val MockUserAuthorizationsString = "A,B,C"
    val MockUserAuthorizations = new Authorizations(
      MockUserAuthorizationsString.split(",").map(_.getBytes()).toList.asJava
    )

    // assign some default authorizations to this mock user
    val connector = {
      val mockInstance = new MockInstance("mycloud")
      val mockConnector = mockInstance.getConnector("user", new PasswordToken("password"))
      mockConnector.securityOperations().changeUserAuthorizations("user", MockUserAuthorizations)
      mockConnector
    }

    val (ds, sft) = {
      val sftName = "CommonSimpleFeatureType"
      val sft = SimpleFeatureTypes.createType(sftName, "*where:Point,when:Date,s:String")
      sft.setTableSharing(true)
      Some("when").foreach(sft.setDtgField)
      val ds = DataStoreFinder.getDataStore(Map(
        "connector" -> connector,
        "caching"   -> false,
        // note the table needs to be different to prevent testing errors
        "tableName" -> sftName).asJava).asInstanceOf[AccumuloDataStore]
      ds.createSchema(sft)
      (ds, ds.getSchema(sftName)) // reload the sft from the ds to ensure all user data is set properly
    }

    /**********************
     * End copied section *
     **********************/

    val n = 250
    val rng = new scala.util.Random

    val x1 = (rng.nextDouble * 355) - 180.0
    val y1 = (rng.nextDouble * 175) - 90.0
    val x2 = x1 + 180*math.pow(2,-30)
    val y2 = y1 + 180*math.pow(2,-30)

    val textFilter = s"BBOX(geom, $x1, $y1, $x2, $y2) AND (dtg DURING 1970-01-03T00:00:00.000Z/1970-01-04T00:00:00.000Z)"
    val query = new Query(textFilter)

    val filter = QueryFilter(StrategyType.Z3, Some(ECQL.toFilter(textFilter)))
    val strategy = new Z3IdxStrategy(filter)

    val qp = QueryPlanner(sft, ds)
    val hints = query.getHints
    val output = ExplainNull

    val result = strategy.getQueryPlan(qp, hints, output)

    println(result)
  }
}
