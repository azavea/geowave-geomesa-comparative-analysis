package com.azavea.ingest.geomesa

import org.apache.spark.rdd._
import org.opengis.feature.simple._
import com.azavea.ingest.common._
import org.geotools.feature.simple._
import org.geotools.data.{DataStoreFinder, DataUtilities, FeatureWriter, Transaction}

object Main {
  def main(args: Array[String]): Unit = {
    val params = CommandLine.parser.parse(args, Ingest.Params()) match {
      case Some(p) => p
      case None => throw new Exception("provide the right arguments, ya goof")
    }

    params.csvOrShp match {
      case Ingest.SHP => {
        val urls = HydrateRDD.getShpUrls(params.s3bucket, params.s3prefix)
        val shpRdd: RDD[SimpleFeature] = HydrateRDD.normalizeShpRdd(HydrateRDD.shpUrls2shpRdd(urls), params.featureName)

        Ingest.registerSFT(params)(shpRdd.first.getType)
        Ingest.ingestRDD(params)(shpRdd)
      }
      case Ingest.CSV => {
        val urls = HydrateRDD.getCsvUrls(params.s3bucket, params.s3prefix, params.csvExtension)
        val tybuilder = new SimpleFeatureTypeBuilder
        tybuilder.setName(params.featureName)
        params.codec.genSFT(tybuilder)
        val sft = tybuilder.buildFeatureType
        val builder = new SimpleFeatureBuilder(sft)
        val csvRdd: RDD[SimpleFeature] = HydrateRDD.csvUrls2Rdd(urls, params.featureName, params.codec, params.dropLines, params.separator)

        Ingest.registerSFT(params)(sft)
        Ingest.ingestRDD(params)(csvRdd)
      }
    }
  }
}
