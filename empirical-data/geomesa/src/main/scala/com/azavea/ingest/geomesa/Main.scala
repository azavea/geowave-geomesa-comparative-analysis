package com.azavea.ingest.geomesa

import org.opengis.feature.simple._
import org.geotools.feature.simple._
import org.geotools.data.{DataStoreFinder, DataUtilities, FeatureWriter, Transaction}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd._

import geotrellis.spark.util.SparkUtils

import com.azavea.ingest.common._
import com.azavea.ingest.common.csv.HydrateRDD._
import com.azavea.ingest.common.shp.HydrateRDD._

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
      case Ingest.SHP => {
        val urls = getShpUrls(params.s3bucket, params.s3prefix)
        val shpUrlRdd = shpUrlsToRdd(urls)
        val shpSimpleFeatureRdd: RDD[SimpleFeature] = NormalizeRDD.normalizeFeatureName(shpUrlRdd, params.featureName)

        Ingest.registerSFT(params)(shpSimpleFeatureRdd.first.getType)
        Ingest.ingestRDD(params)(shpSimpleFeatureRdd)
      }
      case Ingest.CSV => {
        val urls = getCsvUrls(params.s3bucket, params.s3prefix, params.csvExtension)
        val tybuilder = new SimpleFeatureTypeBuilder
        tybuilder.setName(params.featureName)
        params.codec.genSFT(tybuilder)
        val sft = tybuilder.buildFeatureType
        val csvRdd: RDD[SimpleFeature] = csvUrlsToRdd(urls, params.featureName, params.codec, params.dropLines, params.separator)

        Ingest.registerSFT(params)(sft)
        Ingest.ingestRDD(params)(csvRdd)
      }
    }
  }
}

