package com.azavea.ingest.geowave

import mil.nga.giat.geowave.adapter.vector._
import mil.nga.giat.geowave.core.geotime.ingest._
import mil.nga.giat.geowave.core.store.index._
import mil.nga.giat.geowave.datastore.accumulo._
import mil.nga.giat.geowave.datastore.accumulo.index.secondary._
import mil.nga.giat.geowave.datastore.accumulo.metadata._
import mil.nga.giat.geowave.datastore.accumulo.operations.config.AccumuloOptions
import mil.nga.giat.geowave.datastore.accumulo.util.AccumuloUtils
import mil.nga.giat.geowave.core.geotime.index.dimension._
import mil.nga.giat.geowave.core.geotime.index.dimension.TemporalBinningStrategy.{ Unit => BinUnit }
import mil.nga.giat.geowave.core.geotime.ingest._
import mil.nga.giat.geowave.core.index.ByteArrayId
import mil.nga.giat.geowave.core.index.StringUtils
import mil.nga.giat.geowave.core.index.dimension.NumericDimensionDefinition
import mil.nga.giat.geowave.core.index.sfc.SFCDimensionDefinition
import mil.nga.giat.geowave.core.index.sfc.SFCFactory.SFCType
import mil.nga.giat.geowave.core.index.sfc.tiered.TieredSFCIndexFactory
import mil.nga.giat.geowave.core.index.CompoundIndexStrategy
import mil.nga.giat.geowave.core.index.simple.HashKeyIndexStrategy
import mil.nga.giat.geowave.core.index.simple.RoundRobinKeyIndexStrategy
import mil.nga.giat.geowave.core.store.DataStore
import mil.nga.giat.geowave.core.store.index.{PrimaryIndex, CustomIdIndex}
import mil.nga.giat.geowave.core.store.index.writer.IndexWriter
import mil.nga.giat.geowave.core.store.operations.remote.options.IndexPluginOptions
import mil.nga.giat.geowave.core.store.operations.remote.options.IndexPluginOptions.PartitionStrategy
import mil.nga.giat.geowave.datastore.accumulo._
import org.geotools.data.{DataStoreFinder, FeatureSource}
import org.geotools.data.simple.SimpleFeatureStore
import org.geotools.feature.FeatureCollection
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}
import org.geotools.feature.simple.{SimpleFeatureBuilder, SimpleFeatureTypeBuilder}
import org.opengis.filter.Filter
import org.apache.spark.rdd._
import geotrellis.vector.Point

import java.util.HashMap
import scala.collection.JavaConversions._
import scala.util.Try

import com.azavea.ingest.common._

object Ingest {
  trait CSVorSHP
  case object CSV extends CSVorSHP
  case object SHP extends CSVorSHP
  case object AVRO extends CSVorSHP
  implicit val readsCSVorSHP = scopt.Read.reads[CSVorSHP]({ s: String =>
    s.toLowerCase match {
      case "csv" => CSV
      case "shp" => SHP
      case "shapefile" => SHP
      case _ => throw new IllegalArgumentException("Must choose either CSV or SHP")
    }
  })

  case class Params (csvOrShp: CSVorSHP = CSV,
                     instanceId: String = "geowave",
                     zookeepers: String = "zookeeper",
                     user: String = "root",
                     password: String = "GisPwd",
                     tableName: String = "",
                     dropLines: Int = 0,
                     separator: String = "\t",
                     codec: CSVSchemaParser.Expr = CSVSchemaParser.Spec(Nil),
                     featureName: String = "default-feature-name",
                     s3bucket: String = "",
                     s3prefix: String = "",
                     csvExtension: String = ".csv",
                     inputPartitionSize: Int = 10,
                     translationPoints: Seq[Point] = Seq.empty,
                     translationOrigin: Option[Point] = None,
                     temporal: Boolean = false,
                     pointOnly: Boolean = false,
                     numPartitions: Int = 1,
                     partitionStrategy: String = "NONE",
                     numSplits: Option[Int] = None)

  def registerSFT(params: Params)(sft: SimpleFeatureType): Unit = ???

  def getOperations(conf: Params): BasicAccumuloOperations =
    new BasicAccumuloOperations(conf.zookeepers,
      conf.instanceId,
      conf.user,
      conf.password,
      conf.tableName)

  def getGeowaveDataStore(conf: Params): AccumuloDataStore = {
    // GeoWave persists both the index and data adapter to the same accumulo
    // namespace as the data. The intent here
    // is that all data is discoverable without configuration/classes stored
    // outside of the accumulo instance.
    val instance = getOperations(conf)

    val options = new AccumuloOptions
    options.setPersistDataStatistics(true)
    //options.setUseAltIndex(true)

    return new AccumuloDataStore(
      new AccumuloIndexStore(instance),
      new AccumuloAdapterStore(instance),
      new AccumuloDataStatisticsStore(instance),
      new AccumuloSecondaryIndexDataStore(instance),
      new AccumuloAdapterIndexMappingStore(instance),
      instance,
      options)
  }

  def indexes(params: Params): Seq[PrimaryIndex] = {
    val idxs =
      if (params.temporal) {
        // Create both a spatialtemporal and a spatail-only index
        val b = new SpatialTemporalDimensionalityTypeProvider.SpatialTemporalIndexBuilder
        b.setPointOnly(params.pointOnly)
        Seq((new SpatialDimensionalityTypeProvider.SpatialIndexBuilder).createIndex, b.createIndex)
      } else {
        Seq((new SpatialDimensionalityTypeProvider.SpatialIndexBuilder).createIndex)
      }

    if(params.partitionStrategy != "NONE") {
      if(params.numPartitions <= 1) { sys.error("If partition strategy is set, need more than one partition.") }
      val partitionStrategy =
        if(params.partitionStrategy == "ROUND_ROBIN") { PartitionStrategy.ROUND_ROBIN }
        else if (params.partitionStrategy == "HASH") { PartitionStrategy.HASH }
        else { sys.error(s"Partition strategy was ${params.partitionStrategy}, needs to be either ROUND_ROBIN or HASH") }

      // Can only set spatial via this method,
      // because of bug found here: https://github.com/ngageoint/geowave/issues/896
      compoundPartitioningIndex(idxs.head, params.numPartitions, partitionStrategy) +: idxs.tail
    } else {
      idxs
    }
  }

  def ingestRDD(params: Params)(rdd: RDD[SimpleFeature]) = {

    rdd.foreachPartition({ featureIter =>
      val features = featureIter.buffered
      val ds = getGeowaveDataStore(params)

      val adapter = new FeatureDataAdapter(features.head.getType())

      val indexWriter = ds.createWriter(adapter, indexes(params):_*).asInstanceOf[IndexWriter[SimpleFeature]]
      try {
        features.foreach({ feature => indexWriter.write(feature) })
      } finally {
        indexWriter.close()
      }
    })

    // Set the number of splits if the opiton was provided
    params.numSplits match {
      case Some(numSplits) =>
        val ops = getOperations(params)
        val connector = ops.getConnector
        for(index <- indexes(params)) {
          val tableName = AccumuloUtils.getQualifiedTableName(params.tableName, StringUtils.stringFromBinary(index.getId().getBytes()))
          println(s"Setting up splits for $tableName...")
          AccumuloUtils.setSplitsByNumSplits(connector, params.tableName, index, numSplits)
        }
      case None => Unit
    }
  }

  def compoundPartitioningIndex(index: PrimaryIndex, numPartitions: Int, partitionStrategy: IndexPluginOptions.PartitionStrategy): PrimaryIndex = {
    if(numPartitions > 1 && partitionStrategy.equals(PartitionStrategy.ROUND_ROBIN)) {
      new CustomIdIndex(
	new CompoundIndexStrategy(
	  new RoundRobinKeyIndexStrategy(numPartitions),
	  index.getIndexStrategy()
        ),
	index.getIndexModel(),
	new ByteArrayId(
	  index.getId().getString() + "_" + PartitionStrategy.ROUND_ROBIN.name() + "_" + numPartitions
        )
      )
    } else if(numPartitions > 1 && partitionStrategy.equals(PartitionStrategy.HASH)) {
      new CustomIdIndex(
	new CompoundIndexStrategy(
	  new HashKeyIndexStrategy(
	    index.getIndexStrategy().getOrderedDimensionDefinitions(),
	    numPartitions
          ),
	  index.getIndexStrategy()
        ),
	index.getIndexModel(),
	new ByteArrayId(
	  index.getId().getString() + "_" + PartitionStrategy.HASH.name() + "_" + numPartitions
        )
      )
    } else {
      sys.error(s"numPartitions: $numPartitions, partitionStrategy: $partitionStrategy")
    }
  }
}
