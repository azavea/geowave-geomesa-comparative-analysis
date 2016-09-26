package com.azavea.ca.planning

import com.vividsolutions.jts.geom._
import com.vividsolutions.jts.geom.GeometryFactory
import mil.nga.giat.geowave.adapter.vector._
import mil.nga.giat.geowave.core.geotime.index.dimension._
import mil.nga.giat.geowave.core.geotime.index.dimension.TemporalBinningStrategy.{ Unit => BinUnit }
import mil.nga.giat.geowave.core.geotime.ingest._
import mil.nga.giat.geowave.core.geotime.store.query._
import mil.nga.giat.geowave.core.index.dimension.NumericDimensionDefinition
import mil.nga.giat.geowave.core.index.IndexMetaData
import mil.nga.giat.geowave.core.index.sfc.SFCDimensionDefinition
import mil.nga.giat.geowave.core.index.sfc.SFCFactory.SFCType
import mil.nga.giat.geowave.core.index.sfc.tiered.TieredSFCIndexFactory
import mil.nga.giat.geowave.core.store.DataStore
import mil.nga.giat.geowave.core.store.index._
import mil.nga.giat.geowave.core.store.index.PrimaryIndex
import mil.nga.giat.geowave.core.store.index.writer.IndexWriter
import mil.nga.giat.geowave.datastore.accumulo._
import mil.nga.giat.geowave.datastore.accumulo.index.secondary._
import mil.nga.giat.geowave.datastore.accumulo.metadata._
import mil.nga.giat.geowave.datastore.accumulo.operations.config.AccumuloOptions

import scala.math.pow

object WavePlan {

  def main(args: Array[String]): Unit = {

    val n = 250
    val rng = new scala.util.Random

    val MAX_RANGE_DECOMPOSITION = 5000

    val geometryFactory = new GeometryFactory
    val DIMENSIONS = Array[SFCDimensionDefinition](
      new SFCDimensionDefinition(new LongitudeDefinition, 20),
      new SFCDimensionDefinition(new LatitudeDefinition(true), 20),
      new SFCDimensionDefinition(new TimeDefinition(BinUnit.YEAR), 20)
    )
    // val DIMENSIONS = Array[SFCDimensionDefinition](
    //   new SFCDimensionDefinition(new LongitudeDefinition, 31),
    //   new SFCDimensionDefinition(new LatitudeDefinition(true), 31)
    // )
    val strategy = TieredSFCIndexFactory.createSingleTierStrategy(DIMENSIONS, SFCType.HILBERT)

    (-30 to -5).foreach({ bits =>
      val times = (0 to n).map({ _ =>
        val x = (rng.nextDouble * 355) - 180.0
        val y = (rng.nextDouble * 175) - 90.0
        val envelope = new Envelope(
          x, x + 180*math.pow(2,bits),
          y, y + 180*math.pow(2,bits)
        )
        val geom = geometryFactory.toGeometry(envelope)
        val start = new java.util.GregorianCalendar(1970, 0, 2).getTime
        val end = new java.util.GregorianCalendar(1970, 0, 3).getTime
        val query = new SpatialTemporalQuery(start, end, geom)
        // val query = new SpatialQuery(geom)

        val numericData = query.getIndexConstraints(strategy)

        val before = System.currentTimeMillis
        val ranges = strategy.getQueryRanges(numericData.get(0), MAX_RANGE_DECOMPOSITION, null).size
        val after = System.currentTimeMillis

        after - before
      }).drop(1)

      println(times.sum / n.toDouble)
    })
  }
}
