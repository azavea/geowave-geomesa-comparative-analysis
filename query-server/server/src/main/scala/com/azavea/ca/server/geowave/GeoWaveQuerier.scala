package com.azavea.ca.server.geowave

import com.azavea.ca.core._
import com.azavea.ca.server._
import com.azavea.ca.server.geowave.connection._

import com.vividsolutions.jts.geom._
import com.vividsolutions.jts.geom.GeometryFactory
import geotrellis.vector.Extent
import mil.nga.giat.geowave.adapter.vector.FeatureDataAdapter
import mil.nga.giat.geowave.core.geotime.store.query._
import mil.nga.giat.geowave.core.store.CloseableIterator
import mil.nga.giat.geowave.core.store.query._
import mil.nga.giat.geowave.core.store.query.aggregate._
import org.opengis.feature.simple._

import scala.collection.JavaConversions._


case class GeoWaveQuerier(gwNamespace: String, typeName: String) {
  val ds = GeoWaveConnection.dataStore(gwNamespace)
  val adapter = GeoWaveConnection.adapterStore(gwNamespace)
    .getAdapters
    .map(_.asInstanceOf[FeatureDataAdapter])
    .filter(_.getType.getTypeName == typeName)
    .next
  val queryOptions = new QueryOptions(adapter)

  def spatialQuery(geom: geotrellis.vector.Geometry): CloseableIterator[SimpleFeature] =
    spatialQuery(geom.jtsGeom)

  def spatialQuery(geom: Geometry): CloseableIterator[SimpleFeature] =
    ds.query(queryOptions, new SpatialQuery(geom))

  def spatialQueryCount(geom: geotrellis.vector.Geometry): CloseableIterator[CountResult] =
    spatialQueryCount(geom.jtsGeom)

  def spatialQueryCount(geom: Geometry): CloseableIterator[CountResult] = {
    val countQueryOptions = new QueryOptions(adapter)
    countQueryOptions.setAggregation(new CountAggregation, adapter)
    ds.query(countQueryOptions, new SpatialQuery(geom))
  }

  def timeQueryToTemporalRange(tq: TimeQuery): TemporalRange =
    tq match {
      case TimeQuery(Some(from), Some(to)) =>
        new TemporalRange(from, to)
      case TimeQuery(None, Some(to)) =>
        new TemporalRange(TemporalRange.START_TIME, to)
      case TimeQuery(Some(from), None) =>
        new TemporalRange(from, TemporalRange.END_TIME)
      case _ => sys.error("Cannot create geowave temporal query from empty time range")
    }

  def timeQueryToTemporalQuery(tq: TimeQuery): TemporalQuery =
    new TemporalQuery(new TemporalConstraints(timeQueryToTemporalRange(tq), "range"))

  def temporalQuery(tq: TimeQuery): CloseableIterator[SimpleFeature] =
    ds.query(queryOptions, timeQueryToTemporalQuery(tq))

  def temporalQueryCount(tq: TimeQuery): CloseableIterator[CountResult] = {
    val countQueryOptions = new QueryOptions(adapter)
    countQueryOptions.setAggregation(new CountAggregation, adapter)
    ds.query(countQueryOptions, timeQueryToTemporalQuery(tq))
  }

  def spatialTemporalQuery(geom: Geometry, tq: TimeQuery): CloseableIterator[SimpleFeature] = {
    val tr = timeQueryToTemporalRange(tq)
    ds.query(queryOptions, new SpatialTemporalQuery(tr.getStartTime, tr.getEndTime, geom))
  }

  def spatialTemporalQueryCount(geom: Geometry, tq: TimeQuery): CloseableIterator[CountResult] = {
    val countQueryOptions = new QueryOptions(adapter)
    countQueryOptions.setAggregation(new CountAggregation, adapter)
    val tr = timeQueryToTemporalRange(tq)
    ds.query(countQueryOptions, new SpatialTemporalQuery(tr.getStartTime, tr.getEndTime, geom))
  }
}
