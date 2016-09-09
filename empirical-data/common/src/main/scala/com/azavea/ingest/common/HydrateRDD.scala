package com.azavea.ingest.common

import org.apache.spark._
import org.apache.spark.rdd.RDD
import org.geotools.data.DataStoreFinder
import org.geotools.data.simple.SimpleFeatureStore
import org.opengis.feature.simple._
import org.geotools.feature.simple._
import org.geotools.feature._
import com.amazonaws.auth._
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{ListObjectsRequest, ObjectListing}

import java.util.HashMap
import scala.collection.JavaConverters._
import scala.collection.mutable

object HydrateRDD {
  val cred = new DefaultAWSCredentialsProviderChain()
  val client = new AmazonS3Client(cred)

  // Copied from GeoTrellis codebase
  def listKeys(listObjectsRequest: ListObjectsRequest): Seq[String] = {
    var listing: ObjectListing = null
    val result = mutable.ListBuffer[String]()
    do {
      listing = client.listObjects(listObjectsRequest)
      // avoid including "directories" in the input split, can cause 403 errors on GET
      result ++= listing.getObjectSummaries.asScala.map(_.getKey).filterNot(_ endsWith "/")
      listObjectsRequest.setMarker(listing.getNextMarker)
    } while (listing.isTruncated)

    result.toSeq
  }

  def getShpUrls(s3bucket: String, s3prefix: String): Array[String] = {
    val objectRequest = (new ListObjectsRequest)
      .withBucketName(s3bucket)
      .withPrefix(s3prefix)

    listKeys(objectRequest)
      .collect({ case key if key.endsWith(".shp") =>
        s"https://s3.amazonaws.com/${s3bucket}/${key}"
      }).toArray
  }

  def shpUrlsToRdd(urlArray: Array[String])(implicit sc: SparkContext): RDD[SimpleFeature] = {
    val urlRdd: RDD[String] = sc.parallelize(urlArray, urlArray.size / 10)
    urlRdd.mapPartitions({ urlIter =>
      val urls = urlIter.toList
      urls.map({ url =>
        val datastoreParams = Map("url" -> url).asJava
        val shpDS = DataStoreFinder.getDataStore(datastoreParams)
        if (shpDS == null) {
          println("Could not build ShapefileDataStore")
          java.lang.System.exit(-1)
        }
        val featureCollection = shpDS.getFeatureSource(shpDS.getNames().asScala(0)).getFeatures
        featureCollection.toArray.asInstanceOf[Array[SimpleFeature]]
      }).flatten.iterator
    })
  }

  def convertToSFT(sft: SimpleFeatureType)(orig: SimpleFeature): SimpleFeature = {
    val types = sft.getTypes
    val builder = new SimpleFeatureBuilder(sft)
    for (ty <- types.asScala) {
      builder.add(orig.getAttribute(ty.getName))
    }
    orig.getUserData.asScala.foreach { case (k, v) => builder.userData(k, v) }
    builder.buildFeature(orig.getID)
  }

  def normalizeShpRdd(rdd: RDD[SimpleFeature], typeName: String)(implicit sc: SparkContext) =
    rdd.mapPartitions({ featureIter =>
      val bufferedFeatures = featureIter.buffered
      val headFeature = bufferedFeatures.head

      val builder = new SimpleFeatureTypeBuilder
      builder.addAll(headFeature.getType.getAttributeDescriptors)
      builder.setName(typeName)
      val sft = builder.buildFeatureType()

      bufferedFeatures.map({ feature =>
        SimpleFeatureBuilder.retype(feature, sft)
      })
    })

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

        featureCollection.asScala
      }).iterator
    })
  }
}
