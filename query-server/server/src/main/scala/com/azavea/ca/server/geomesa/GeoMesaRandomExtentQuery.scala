package com.azavea.ca.server.geomesa

import connection._
import com.azavea.ca.server._

import org.geotools.data.DataStoreFinder
import org.geotools.data.{ Query => GeoToolsQuery }
import org.geotools.filter.text.cql2.CQL
import org.geotools.filter.text.cql2.CQLException
import org.locationtech.geomesa.accumulo.data.AccumuloDataStore
import org.opengis.filter.Filter


object GeoMesaRandomExtentQuery {

  /**
    * Perform various types of queries:
    *    - Spatial or Spatio-Temporal
    *    - Point (so-called "stabbing queries") or Range
    *
    * @param tableName  The GeoMesa table name to query
    * @param typeName   The name of the SimpleFeatureType to query for
    * @param where      The field in the SimpleFeatureType that contains the geometry
    * @param lowerLeft  The lower-left point of the bbox or the query point
    * @param upperRight The upper-right point of the bbox (optional)
    * @param when       The field in the SimpleFeatureType that contans the time (optional)
    * @param fromTime   The beginning of the time interval (optional)
    * @param toTime     The end of the time interval (optional)
    * @return           The number of records found
    */
  def query(
    tableName: String,
    typeName: String,
    where: String,
    lowerLeft: Seq[Double],
    upperRight: Option[Seq[Double]],
    when: Option[String],
    fromTime: Option[String],
    toTime: Option[String]
  ): Int = {

    val ds = GeoMesaConnection.dataStore(tableName)
    val spatial = upperRight match {
      case Some(upperRight) =>
        s"BBOX($where, ${lowerLeft(0)}, ${lowerLeft(1)}, ${upperRight(0)}, ${upperRight(1)})"
      case None =>
        s"INTERSECTS($where, POINT( ${lowerLeft(0)} ${lowerLeft(1)} ))"
    }
    val temporal = (when, fromTime, toTime) match {
      case (Some(when), Some(fromTime), Some(toTime)) =>
        s"AND ($when DURING ${fromTime}/${toTime})"
      case (Some(when), Some(fromTime), _) =>
        s"AND ($when TEQUALS $fromTime)"
      case _ => ""
    }
    // println(s"MESA ${spatial + temporal}")

    val filter = CQL.toFilter(spatial + temporal)
    val query = new GeoToolsQuery(typeName, filter)
    val fs = ds.getFeatureSource(typeName)
    val itr = fs.getFeatures(query).features

    var n = 0
    while (itr.hasNext) {
      val feature = itr.next
      // println(s"MESA ${feature.getProperties}")
      n += 1
    }
    n // return value
  }

}
