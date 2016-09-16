package com.azavea.ingest.geomesa

import org.apache.spark._
import org.apache.spark.rdd._
import org.opengis.feature.simple._
import org.geotools.feature.simple._
import org.geotools.data.{DataStoreFinder, DataUtilities, FeatureWriter, Transaction}
import org.geotools.referencing.CRS;
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd._

import geotrellis.spark.util.SparkUtils

import com.azavea.ingest.common._
import com.azavea.ingest.common.csv.HydrateRDD._
import com.azavea.ingest.common.shp.HydrateRDD._
import com.azavea.ingest.common.avro.HydrateRDD._


object Main {
  def main(args: Array[String]): Unit = {
    val params = CommandLine.parser.parse(args, Ingest.Params()) match {
      case Some(p) => p
      case None => throw new Exception("provide the right arguments, ya goof")
    }

    val conf: SparkConf =
      new SparkConf()
        .setAppName("GeoMesa ingest utility")
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
        println(s"\n\nNUMBER OF URLS = ${urls.size}")
        val shpUrlRdd = shpUrlsToRdd(urls, params.inputPartitionSize)
        val shpSimpleFeatureRdd: RDD[SimpleFeature] = NormalizeRDD.normalizeFeatureName(shpUrlRdd, params.featureName)
        val reprojected = shpSimpleFeatureRdd.map(Reproject(_, CRS.decode("EPSG:4326")))

        if (params.translationPoints.nonEmpty && params.translationOrigin.isDefined)
          Ingest.ingestRDD(params)(
            TranslateRDD(reprojected, params.translationOrigin.get, params.translationPoints))
        else
          Ingest.ingestRDD(params)(reprojected)
      }
      case Ingest.CSV => {
        val urls = Util.listKeys(params.s3bucket, params.s3prefix, params.csvExtension)
        println(s"\n\nNUMBER OF URLS = ${urls.size}")

        val tybuilder = new SimpleFeatureTypeBuilder
        tybuilder.setName(params.featureName)
        params.codec.genSFT(tybuilder)
        val sft = tybuilder.buildFeatureType
        val csvRdd: RDD[SimpleFeature] = csvUrlsToRdd(urls, params.featureName, params.codec, params.dropLines, params.separator)

        Ingest.ingestRDD(params)(csvRdd)
      }
    }
  }
}
