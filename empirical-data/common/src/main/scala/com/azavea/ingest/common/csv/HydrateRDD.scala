package com.azavea.ingest.common.csv

import org.apache.spark._
import org.apache.spark.rdd.RDD
import org.geotools.data.DataStoreFinder
import org.geotools.data.simple.SimpleFeatureStore
import org.opengis.feature.simple._
import org.geotools.feature.simple._
import org.geotools.feature._
import com.amazonaws.auth._
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ListObjectsRequest

import java.util.HashMap
import scala.collection.JavaConversions._
import scala.collection.mutable

import com.azavea.ingest.common._

object HydrateRDD extends HydrateRDDUtils {
  def getCsvUrls(s3bucket: String, s3prefix: String, extension: String): Array[String] = {
    val objectRequest =
      (new ListObjectsRequest)
      .withBucketName(s3bucket)
      .withPrefix(s3prefix)

    val s3objects = client.listObjects(s3bucket, s3prefix)
    val summaries = s3objects.getObjectSummaries

    listKeys(objectRequest)
      .collect({ case key if key.endsWith(extension) =>
        s"https://s3.amazonaws.com/${s3bucket}/${key}"
      }).toArray
  }


  def csvUrlsToRdd(
    urlArray: Array[String],
    sftName: String,
    schema: CSVSchemaParser.Expr,
    drop: Int,
    delim: String
  )(implicit sc: SparkContext): RDD[SimpleFeature] = {
    val urlRdd: RDD[String] = sc.parallelize(urlArray, urlArray.size / 20)
    urlRdd.mapPartitions({ urlIter =>
      val urls = urlIter.toList
      urls.flatMap({ urlName =>
        val url = new java.net.URL(urlName)
        val featureCollection = new DefaultFeatureCollection(null, null)

        try {
          CSVtoSimpleFeature.parseCSVFile(schema, url, drop, delim, sftName, featureCollection)
        } catch {
          case e: java.io.IOException =>
            println(s"Discarded ${url} (File does not exist?)")
          case e: Exception =>
            println(e.getMessage())
        }

        featureCollection
      }).iterator
    })
  }

}
