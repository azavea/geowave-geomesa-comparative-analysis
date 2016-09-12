package com.azavea.geowave

import com.azavea.common._

import mil.nga.giat.geowave.core.geotime.index.dimension._
import mil.nga.giat.geowave.core.geotime.index.dimension.TemporalBinningStrategy.{ Unit => BinUnit }
import mil.nga.giat.geowave.core.geotime.ingest._
import mil.nga.giat.geowave.core.index.dimension.NumericDimensionDefinition
import mil.nga.giat.geowave.core.index.sfc.SFCDimensionDefinition
import mil.nga.giat.geowave.core.index.sfc.SFCFactory.SFCType
import mil.nga.giat.geowave.core.index.sfc.tiered.TieredSFCIndexFactory
import mil.nga.giat.geowave.core.store.DataStore
import mil.nga.giat.geowave.core.store.index.PrimaryIndex
import mil.nga.giat.geowave.core.store.index.writer.IndexWriter
import mil.nga.giat.geowave.adapter.vector._
import mil.nga.giat.geowave.core.store.index._
import mil.nga.giat.geowave.datastore.accumulo._
import mil.nga.giat.geowave.datastore.accumulo.index.secondary._
import mil.nga.giat.geowave.datastore.accumulo.metadata._
import mil.nga.giat.geowave.datastore.accumulo.operations.config.AccumuloOptions
import mil.nga.giat.geowave.core.store.index.writer.IndexWriter
import org.apache.spark.{SparkConf, SparkContext}
import org.geotools.data.{DataStoreFinder, FeatureSource}
import org.geotools.data.simple.SimpleFeatureStore
import org.geotools.feature.FeatureCollection
import mil.nga.giat.geowave.datastore.accumulo._
import org.apache.spark.{SparkConf, SparkContext}
import org.opengis.feature.simple.SimpleFeature


object WavePoke extends CommonPoke {

  def main(args: Array[String]): Unit = {

    if (args.length < 7) {
      println(s"arguments: <instance> <zookeepers> <user> <password> <gwNamespace> <indexType> <instruction(s)>")
      System.exit(-1)
    }

    // Spark Context
    val sparkConf = (new SparkConf).setAppName("GeoWave Synthetic Data Ingest")
    val sparkContext = new SparkContext(sparkConf)

    // Generate List of Geometries
    val geometries = args.drop(6).toList.flatMap(decode)

    // Store Geometries in GeoWave
    sparkContext
      .parallelize(geometries, geometries.length)
      .foreach({ tuple =>
        val basicOpsInstance = new BasicAccumuloOperations(args(1), args(0), args(2), args(3), args(4))
        val ds = new AccumuloDataStore(basicOpsInstance)

        val index = args(5).split(":") match {
          case Array("spacetime", xbits, ybits, tbits) => {
            /* Create a single-tier index.  Construction cribbed from
             * PersistenceEncodingTest.java and HilbertSFCTest.java in
             * the GeoWave Tree. */
            val SPATIAL_TEMPORAL_DIMENSIONS = Array[SFCDimensionDefinition](
              new SFCDimensionDefinition(new LongitudeDefinition, xbits.toInt),
              new SFCDimensionDefinition(new LatitudeDefinition(true), ybits.toInt),
              new SFCDimensionDefinition(new TimeDefinition(BinUnit.WEEK), tbits.toInt)
            )
            val model = (new SpatialTemporalDimensionalityTypeProvider).createPrimaryIndex.getIndexModel
            val strategy = TieredSFCIndexFactory.createSingleTierStrategy(
              SPATIAL_TEMPORAL_DIMENSIONS,
              SFCType.HILBERT
            )

            new PrimaryIndex(strategy, model)
          }
          case Array("space", xbits, ybits) => {
            val SPATIAL_DIMENSIONS = Array[SFCDimensionDefinition](
              new SFCDimensionDefinition(new LongitudeDefinition, xbits.toInt),
              new SFCDimensionDefinition(new LatitudeDefinition(true), ybits.toInt)
            )
            val model = (new SpatialDimensionalityTypeProvider).createPrimaryIndex.getIndexModel
            val strategy = TieredSFCIndexFactory.createSingleTierStrategy(
              SPATIAL_DIMENSIONS,
              SFCType.HILBERT
            )

            new PrimaryIndex(strategy, model)
          }
          case Array("space") =>
            (new SpatialDimensionalityTypeProvider.SpatialIndexBuilder)
              .createIndex
          case Array("spacetime") =>
            (new SpatialTemporalDimensionalityTypeProvider.SpatialTemporalIndexBuilder)
              .createIndex
          case _ =>
            throw new Exception("Unrecognized index type ${args(5)}")
        }

        val schema = tuple._1 match {
          case `either` => CommonSimpleFeatureType("Geometry")
          case `extent` => CommonSimpleFeatureType("Polygon")
          case `point` => CommonSimpleFeatureType("Point")
        }

        val adapter = new FeatureDataAdapter(schema)
        val indexWriter = ds.createWriter(adapter, index).asInstanceOf[IndexWriter[SimpleFeature]]

        val fc = tuple match {
          case (_, seed: Long, lng: String, lat: String, time: String, width: String) =>
            GeometryGenerator(schema, seed, lng, lat, time, width)
        }
        val itr = fc.features

        while (itr.hasNext) { indexWriter.write(itr.next) }
        itr.close
        indexWriter.close
      })

    sparkContext.stop

  }

}
