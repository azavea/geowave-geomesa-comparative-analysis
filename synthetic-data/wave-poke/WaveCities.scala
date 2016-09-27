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


object WaveCities extends CommonPoke {

  def main(args: Array[String]): Unit = {
    val conf = CommandLine.parser.parse(args, CommandLine.DEFAULT_OPTIONS).get

    // Spark Context
    val sparkConf = (new SparkConf).setAppName("GeoWave Synthetic Data Ingest")
    val sparkContext = new SparkContext(sparkConf)

    // Generate List of Geometries
    val geometries = Cities.geometries(conf.instructions)

    // Store Geometries in GeoWave
    sparkContext
      .parallelize(geometries, geometries.length)
      .foreach { tuple =>
        val basicOpsInstance = new BasicAccumuloOperations(
          conf.zookeepers, conf.instanceId, conf.user, conf.password, conf.tableName)
        val ds = new AccumuloDataStore(basicOpsInstance)

        val index = WavePoke.parseIndex(conf.index)
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
      }

    sparkContext.stop
  }
}
