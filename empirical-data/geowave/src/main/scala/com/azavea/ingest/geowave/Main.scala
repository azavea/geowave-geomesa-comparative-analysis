package com.azavea.ingest.geowave

//import com.typesafe.scalalogging.Logger
import mil.nga.giat.geowave.adapter.vector._
import mil.nga.giat.geowave.core.geotime.ingest._
import mil.nga.giat.geowave.core.{store => geowave}
import mil.nga.giat.geowave.core.store.index._
import mil.nga.giat.geowave.datastore.accumulo._
import mil.nga.giat.geowave.datastore.accumulo.index.secondary._
import mil.nga.giat.geowave.datastore.accumulo.metadata._
import mil.nga.giat.geowave.datastore.accumulo.operations.config.AccumuloOptions
import org.geotools.data.{DataStoreFinder, FeatureSource}
import org.geotools.feature.FeatureCollection
import org.opengis.filter.Filter
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}
import org.geotools.data.simple.SimpleFeatureStore
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd._

import java.util.HashMap
import scala.collection.JavaConversions._
import scala.util.Try

import com.azavea.ingest.common._
import com.azavea.ingest.common.csv.HydrateRDD._
import com.azavea.ingest.common.shp.HydrateRDD._
import com.azavea.ingest.common.avro.HydrateRDD._

object Main {
  def main(args: Array[String]): Unit = {
    val params = CommandLine.parser.parse(args, Ingest.Params()) match {
      case Some(p) => p
      case None => {
        java.lang.System.exit(0)
        Ingest.Params()
      }
    }

    // Setup Spark environment
    val conf: SparkConf =
      new SparkConf()
        .setAppName("GeoWave ingest utility")
        .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
        .set("spark.kryo.registrator", "geotrellis.spark.io.kryo.KryoRegistrator")

    implicit val sc = new SparkContext(conf)

    params.csvOrShp match {
      case Ingest.AVRO =>
        val urls = Util.listKeysS3(params.s3bucket, params.s3prefix, ".avro")
        val rdd = avroUrlsToRdd(params.featureName, urls, params.inputPartitionSize)

        if (params.translationPoints.nonEmpty && params.translationOrigin.isDefined)
          Ingest.ingestRDD(params)(
            TranslateRDD(rdd, params.translationOrigin.get, params.translationPoints))
        else
          Ingest.ingestRDD(params)(rdd)

      case Ingest.SHP => {
        val urls = Util.listKeys(params.s3bucket, params.s3prefix, ".shp")
        val shpUrlRdd: RDD[SimpleFeature] = shpUrlsToRdd(urls, params.inputPartitionSize)
        val shpSimpleFeatureRdd: RDD[SimpleFeature] = NormalizeRDD.normalizeFeatureName(shpUrlRdd, params.featureName)

        if (params.translationPoints.nonEmpty && params.translationOrigin.isDefined)
          Ingest.ingestRDD(params)(
            TranslateRDD(shpSimpleFeatureRdd, params.translationOrigin.get, params.translationPoints))
        else
          Ingest.ingestRDD(params)(shpSimpleFeatureRdd)
      }
      case Ingest.CSV => {
        val urls = Util.listKeys(params.s3bucket, params.s3prefix, params.csvExtension)
        val csvRdd: RDD[SimpleFeature] = csvUrlsToRdd(urls, params.featureName, params.codec, params.dropLines, params.separator)

        Ingest.ingestRDD(params)(csvRdd)
      }
    }
  }
}
