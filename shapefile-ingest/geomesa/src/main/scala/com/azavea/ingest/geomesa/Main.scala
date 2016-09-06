package com.azavea.ingest.geomesa

import org.apache.spark.rdd._
import org.opengis.feature.simple._
import com.azavea.ingest.common._
import org.geotools.feature.simple._

object Main {
  def main(args: Array[String]): Unit = {
    val params = Ingest.parser.parse(args, Ingest.Params()) match {
      case Some(p) => p
      case None => throw new Exception("provide the right arguments, ya goof")
    }

    val urls = HydrateRDD.getCsvUrls(params.s3bucket, params.s3prefix, params.csvExtension)
    val builder = new SimpleFeatureBuilder(params.tyBuilder.buildFeatureType)
    val csvRdd: RDD[SimpleFeature] = HydrateRDD.csvUrls2Rdd(urls, builder, params.codec, params.dropLines, params.separator)
  }
}
