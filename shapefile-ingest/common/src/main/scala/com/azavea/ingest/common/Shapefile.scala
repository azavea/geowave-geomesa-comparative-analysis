package com.azavea.ingest.common

import org.apache.spark._
import org.apache.spark.rdd.RDD
import org.geotools.data.DataStoreFinder
import org.geotools.data.simple.SimpleFeatureStore
import org.opengis.feature.simple.SimpleFeature
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ListObjectsRequest

import java.util.HashMap
import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.util.Try

object HydrateRDD {

  def getShpUrls(s3bucket: String, s3prefix: String): Array[String] = {
    val cred = new ProfileCredentialsProvider()
    val client = new AmazonS3Client(cred)

    val objectRequest = (new ListObjectsRequest)
      .withBucketName(s3bucket)
      .withPrefix(s3prefix)
      .withDelimiter("/") // Avoid digging into a deeper directory

    val s3objects = client.listObjects(s3bucket, s3prefix)
    val summaries = s3objects.getObjectSummaries

    s3objects.getObjectSummaries
      .collect({ case summary if summary.getKey.endsWith(".shp") =>
        s"https://s3.amazonaws.com/${summary.getBucketName}/${summary.getKey}"
      }).toArray
  }

  def shpUrls2shpRdd(urlArray: Array[String]): RDD[SimpleFeature] = {
    val sparkConf: SparkConf = (new SparkConf).setAppName("GeoMesa shapefile ingest")
    val sc: SparkContext = new SparkContext(sparkConf)

    val urlRdd: RDD[String] = sc.parallelize(urlArray)
    urlRdd.mapPartitions({ urlIter =>
      val urls = urlIter.toList
      urls.flatMap({ url =>
        val datastoreParams = Map("url" -> url)
        val shpDS = DataStoreFinder.getDataStore(datastoreParams)
        if (shpDS == null) {
          println(s"Discarded ${url} (File does not exist?)")
          None
        } else {
          val featureCollection = shpDS.getFeatureSource(shpDS.getNames()(0)).getFeatures
          Some(featureCollection.toArray.asInstanceOf[Array[SimpleFeature]])
        }
      }).iterator
    })
  }

  def getCsvUrls(s3bucket: String, s3prefix: String, extension: String): Array[String] = {
    val cred = new ProfileCredentialsProvider()
    val client = new AmazonS3Client(cred)

    val objectRequest = (new ListObjectsRequest)
      .withBucketName(s3bucket)
      .withPrefix(s3prefix)
      .withDelimiter("/") // Avoid digging into a deeper directory

    val s3objects = client.listObjects(s3bucket, s3prefix)
    val summaries = s3objects.getObjectSummaries

    s3objects.getObjectSummaries
      .collect({ case summary if summary.getKey.endsWith(extension) =>
        s"https://s3.amazonaws.com/${summary.getBucketName}/${summary.getKey}"
      }).toArray
  }

  def csvUrls2shpRdd(urlArray: Array[String]): RDD[SimpleFeature] = {
    val sparkConf: SparkConf = (new SparkConf).setAppName("GeoMesa shapefile ingest")
    val sc: SparkContext = new SparkContext(sparkConf)

    val urlRdd: RDD[String] = sc.parallelize(urlArray)
    urlRdd.mapPartitions({ urlIter =>
      val urls = urlIter.toList
      urls.flatMap({ urlName =>
        val url = new java.net.URL(urlName)
        val featureCollection = new DefaultFeatureCollection(null, null)

        Try(
        if (shpDS == null) {
          println(s"Discarded ${url} (File does not exist?)")
          None
        } else {
          val featureCollection = shpDS.getFeatureSource(shpDS.getNames()(0)).getFeatures
          Some(featureCollection.toArray.asInstanceOf[Array[SimpleFeature]])
        }
      }).iterator
    })
  }
}
