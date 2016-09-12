package com.azavea.ca.server.geowave

import com.azavea.ca.server._
import com.azavea.ca.server.geowave.connection._

import com.vividsolutions.jts.geom._
import com.vividsolutions.jts.geom.GeometryFactory
import mil.nga.giat.geowave.adapter.vector.FeatureDataAdapter
import mil.nga.giat.geowave.core.geotime.store.query._
import mil.nga.giat.geowave.core.store.CloseableIterator
import mil.nga.giat.geowave.core.store.query.{Query => GeoWaveQuery, QueryOptions}
import mil.nga.giat.geowave.core.store.query.aggregate._
import org.opengis.feature.simple._

import scala.collection.JavaConversions._

object GeoWaveRandomExtentQuery {

  val dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  val geometryFactory = new GeometryFactory

  def query(
    gwNamespace: String,
    typeName: String,
    where: String,
    lowerLeft: Seq[Double],
    upperRight: Option[Seq[Double]],
    when: Option[String],
    fromTime: Option[String],
    toTime: Option[String]
  ): Int = {
    /* Get the DataStore, the DataAdapter, and the QueryOptions */
    val ds = GeoWaveConnection.dataStore(gwNamespace)
    val adapter = GeoWaveConnection.adapterStore(gwNamespace)
      .getAdapters
      .map(_.asInstanceOf[FeatureDataAdapter])
      .filter(_.getType.getTypeName == typeName)
      .next
    val queryOptions = new QueryOptions(adapter)

    /* Get the Query Geometry, either a box or a point */
    val geom = upperRight match {
      case Some(upperRight) =>
        val envelope = new Envelope(upperRight(0), lowerLeft(0), upperRight(1), lowerLeft(1))
        geometryFactory.toGeometry(envelope)
      case _ =>
        val coordinate = new Coordinate(lowerLeft(0), lowerLeft(1))
        geometryFactory.createPoint(coordinate)
    }
    // println(s"WAVE $geom")

    /* Create the Query */
    val query: GeoWaveQuery = (when, fromTime, toTime) match {
      case (Some(when), Some(fromTime), Some(toTime)) =>
        val start = dateFormat.parse(fromTime)
        val end = dateFormat.parse(toTime)
        new SpatialTemporalQuery(start, end, geom)
      case (Some(when), Some(fromTime), _) =>
        val start = dateFormat.parse(fromTime)
        new SpatialTemporalQuery(start, start, geom)
      case _ =>
        new SpatialQuery(geom)
    }

    var n = 0
    val itr = ds.query(queryOptions, query)
    while (itr.hasNext) {
      val feature: SimpleFeature = itr.next
      // println(s"WAVE ${feature}")
      n += 1
    }
    itr.close; n // return value
  }

}
