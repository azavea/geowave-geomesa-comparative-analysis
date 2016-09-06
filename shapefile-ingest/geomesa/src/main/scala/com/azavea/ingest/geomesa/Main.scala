package com.azavea.ingest.geomesa

import org.apache.spark.rdd._
import org.opengis.feature.simple._
import com.azavea.ingest.common._
import org.geotools.feature.simple._
import org.geotools.data.{DataStoreFinder, DataUtilities, FeatureWriter, Transaction}

object Main {
  def main(args: Array[String]): Unit = {
    val params = Ingest.parser.parse(args, Ingest.Params()) match {
      case Some(p) => p
      case None => throw new Exception("provide the right arguments, ya goof")
    }

    val ds = DataStoreFinder.getDataStore(params.convertToJMap)
    if (ds == null) {
      println("Could not build AccumuloDataStore")
      java.lang.System.exit(-1)
    }
    val sftBuilder = new SimpleFeatureTypeBuilder
    sftBuilder.setName(params.featureName)
    params.codec.genSFT(sftBuilder)
    val sft = sftBuilder.buildFeatureType
    ds.createSchema(sft)
    ds.dispose

    val urls = HydrateRDD.getCsvUrls(params.s3bucket, params.s3prefix, params.csvExtension)
    val csvRdd: RDD[SimpleFeature] = HydrateRDD.csvUrls2Rdd(urls, params.featureName, params.codec, params.dropLines, params.separator)
    Ingest.ingestRDD(params, csvRdd)
  }
}
