package com.azavea.ingest.geowave

import mil.nga.giat.geowave.adapter.vector._
import mil.nga.giat.geowave.core.geotime.ingest._
import mil.nga.giat.geowave.core.store.index._
import mil.nga.giat.geowave.datastore.accumulo._
import mil.nga.giat.geowave.datastore.accumulo.index.secondary._
import mil.nga.giat.geowave.datastore.accumulo.metadata._
import mil.nga.giat.geowave.datastore.accumulo.operations.config.AccumuloOptions
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
import mil.nga.giat.geowave.datastore.accumulo._
import org.geotools.data.{DataStoreFinder, FeatureSource}
import org.geotools.data.simple.SimpleFeatureStore
import org.geotools.feature.FeatureCollection
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}
import org.geotools.feature.simple.{SimpleFeatureBuilder, SimpleFeatureTypeBuilder}
import org.opengis.filter.Filter
import org.apache.spark.rdd._

import java.util.HashMap
import scala.collection.JavaConversions._
import scala.util.Try

import com.azavea.ingest.common._

object Ingest {
  trait CSVorSHP
  case object CSV extends CSVorSHP
  case object SHP extends CSVorSHP
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
                     temporal: Boolean = false,
                     pointOnly: Boolean = false,
                     unifySFT: Boolean = true)

  def registerSFT(params: Params)(sft: SimpleFeatureType): Unit = ???

  def getGeowaveDataStore(conf: Params): AccumuloDataStore = {
    // GeoWave persists both the index and data adapter to the same accumulo
    // namespace as the data. The intent here
    // is that all data is discoverable without configuration/classes stored
    // outside of the accumulo instance.
    val instance = new BasicAccumuloOperations(conf.zookeepers,
                                               conf.instanceId,
                                               conf.user,
                                               conf.password,
                                               conf.tableName)

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

  def ingestRDD(params: Params)(rdd: RDD[SimpleFeature], featureCodec: CSVSchemaParser.Expr, sftName: String) =
    rdd.foreachPartition({ featureIter =>
      val ds = getGeowaveDataStore(params)

      val tybuilder = new SimpleFeatureTypeBuilder
      tybuilder.setName(sftName)
      featureCodec.genSFT(tybuilder)
      val sft = tybuilder.buildFeatureType

      val adapter = new FeatureDataAdapter(sft)
      val index =
        if (params.temporal) {
          val b = new SpatialTemporalDimensionalityTypeProvider.SpatialTemporalIndexBuilder
          b.setPointOnly(params.pointOnly)
          b.createIndex
        } else {
          (new SpatialDimensionalityTypeProvider.SpatialIndexBuilder).createIndex
        }
      val indexWriter = ds.createWriter(adapter, index).asInstanceOf[IndexWriter[SimpleFeature]]
      try {
        while (featureIter.hasNext) { indexWriter.write(featureIter.next()) }
      } finally {
        indexWriter.close()
      }
    })


  def ingestShapefileFromURL(params: Params)(url: String) = {
    val shpParams = new HashMap[String, Object]
    shpParams.put("url", url)
    val shpDS = DataStoreFinder.getDataStore(shpParams)
    if (shpDS == null) {
      println("Could not build ShapefileDataStore")
      java.lang.System.exit(-1)
    }

    val typeName = shpDS.getTypeNames()(0)
    val source: FeatureSource[SimpleFeatureType, SimpleFeature] = shpDS.getFeatureSource(typeName)
    val schema = shpDS.getSchema(typeName)
    val collection: FeatureCollection[SimpleFeatureType, SimpleFeature] = source.getFeatures(Filter.INCLUDE)
    val features = collection.features

    val maybeGWDataStore = Try(getGeowaveDataStore(params))
    if (maybeGWDataStore.isFailure) {
      println("Could not connect to Accumulo instance")
      println(maybeGWDataStore)
      java.lang.System.exit(-1)
    }
    val ds = maybeGWDataStore.get

    // Prepare to write to Geowave
    val adapter = new FeatureDataAdapter(schema)
    val index = (new SpatialDimensionalityTypeProvider).createPrimaryIndex
    val indexWriter = ds.createWriter(adapter, index).asInstanceOf[IndexWriter[SimpleFeature]]

    // Write features from shapefile
    var i = 0
    while (features.hasNext()) {
      val feature = features.next()
      indexWriter.write(feature)
      i += 1
    }
    println(s"$i of ${collection.size} features read")

    // Clean up
    features.close
    indexWriter.close
    shpDS.dispose
  }
}
