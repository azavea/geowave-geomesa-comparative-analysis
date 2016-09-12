package com.azavea.ca.server.geowave

import com.azavea.ca.core._
import com.azavea.ca.server._
import com.azavea.ca.server.geowave.connection._

import com.vividsolutions.jts.geom._
import com.vividsolutions.jts.geom.GeometryFactory
import geotrellis.vector.Extent
import mil.nga.giat.geowave.adapter.vector.FeatureDataAdapter
import mil.nga.giat.geowave.core.geotime.ingest.{SpatialTemporalDimensionalityTypeProvider, SpatialDimensionalityTypeProvider}
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

  def spatialQuery(geom: geotrellis.vector.Geometry): CloseableIterator[SimpleFeature] =
    spatialQuery(geom.jtsGeom)

  def spatialQuery(geom: Geometry): CloseableIterator[SimpleFeature] = {
    val queryOptions = new QueryOptions(adapter, (new SpatialDimensionalityTypeProvider.SpatialIndexBuilder).createIndex)
    ds.query(queryOptions, new SpatialQuery(geom))
  }

  def spatialQueryCount(geom: geotrellis.vector.Geometry): CloseableIterator[CountResult] =
    spatialQueryCount(geom.jtsGeom)

  def spatialQueryCount(geom: Geometry): CloseableIterator[CountResult] = {
    val queryOptions = new QueryOptions(adapter, (new SpatialDimensionalityTypeProvider.SpatialIndexBuilder).createIndex)
    queryOptions.setAggregation(new CountAggregation, adapter)
    ds.query(queryOptions, new SpatialQuery(geom))
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

  def timeQueryToTemporalQuery(tq: TimeQuery, pointOnly: Boolean = false): TemporalQuery = {
    new TemporalQuery(new TemporalConstraints(timeQueryToTemporalRange(tq), "range"))
  }

  def temporalQuery(tq: TimeQuery, pointOnly: Boolean = false): CloseableIterator[SimpleFeature] = {
    val b = new SpatialTemporalDimensionalityTypeProvider.SpatialTemporalIndexBuilder
    b.setPointOnly(pointOnly)
    val queryOptions = new QueryOptions(adapter, b.createIndex)

    ds.query(queryOptions, timeQueryToTemporalQuery(tq, pointOnly))
  }

  def temporalQueryCount(tq: TimeQuery, pointOnly: Boolean = false): CloseableIterator[CountResult] = {
    val b = new SpatialTemporalDimensionalityTypeProvider.SpatialTemporalIndexBuilder
    b.setPointOnly(pointOnly)
    val queryOptions = new QueryOptions(adapter, b.createIndex)
    queryOptions.setAggregation(new CountAggregation, adapter)

    ds.query(queryOptions, timeQueryToTemporalQuery(tq))
  }

  def spatialTemporalQuery(geom: Geometry, tq: TimeQuery, pointOnly: Boolean = false): CloseableIterator[SimpleFeature] = {
    val b = new SpatialTemporalDimensionalityTypeProvider.SpatialTemporalIndexBuilder
    b.setPointOnly(pointOnly)
    val queryOptions = new QueryOptions(adapter, b.createIndex)

    val tr = timeQueryToTemporalRange(tq)
    ds.query(queryOptions, new SpatialTemporalQuery(tr.getStartTime, tr.getEndTime, geom))
  }

  def spatialTemporalQueryCount(geom: Geometry, tq: TimeQuery, pointOnly: Boolean = false): CloseableIterator[CountResult] = {
    val b = new SpatialTemporalDimensionalityTypeProvider.SpatialTemporalIndexBuilder
    b.setPointOnly(pointOnly)
    val queryOptions = new QueryOptions(adapter, b.createIndex)
    queryOptions.setAggregation(new CountAggregation, adapter)

    val tr = timeQueryToTemporalRange(tq)
    ds.query(queryOptions, new SpatialTemporalQuery(tr.getStartTime, tr.getEndTime, geom))
  }

}
