package com.azavea.ingest.common

import com.amazonaws.auth._
import com.amazonaws.services.s3.model.{ListObjectsRequest, ObjectListing}
import com.amazonaws.services.s3.{AmazonS3Client, AmazonS3URI}
import java.io._
import java.net._
import java.util.HashMap
import java.util._
import org.apache.commons.io.IOUtils
import org.apache.spark._
import org.apache.spark.rdd.RDD
import org.geotools.data.DataStoreFinder
import org.geotools.data.simple.SimpleFeatureStore
import org.geotools.feature._
import org.geotools.feature.simple._
import org.opengis.feature.simple._
import scala.collection.JavaConverters._
import scala.collection.mutable

object Util {
  val cred = new DefaultAWSCredentialsProviderChain()
  val client = new AmazonS3Client(cred)

  def listKeysS3(s3bucket: String, s3prefix: String, ext: String, recursive: Boolean = false): Array[String] = {
    val objectRequest = (new ListObjectsRequest)
      .withBucketName(s3bucket)
      .withPrefix(s3prefix)

    if (! recursive) { // Avoid digging into a deeper directory
      objectRequest.withDelimiter("/")
    }

    listKeys(objectRequest)
      .collect { case key if key.endsWith(ext) => s"s3://${s3bucket}/${key}" }.toArray
  }

  def listKeys(s3bucket: String, s3prefix: String, ext: String, recursive: Boolean = false): Array[String] = {
    val objectRequest = (new ListObjectsRequest)
      .withBucketName(s3bucket)
      .withPrefix(s3prefix)

    if (! recursive) { // Avoid digging into a deeper directory
      objectRequest.withDelimiter("/")
    }

    listKeys(objectRequest)
      .collect { case key if key.endsWith(ext) =>
        s"https://s3.amazonaws.com/${s3bucket}/${key}"
      }.toArray
  }

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

  def getStream(fileUri: String): InputStream = {
    val uri = new URI(fileUri)

    uri.getScheme match {
      case "file" =>
        new FileInputStream(new File(uri))
      case "http" =>
        uri.toURL.openStream
      case "https" =>
        uri.toURL.openStream
      case "s3" =>
        val client = new AmazonS3Client(new DefaultAWSCredentialsProviderChain)
        val s3uri = new AmazonS3URI(uri)
        client.getObject(s3uri.getBucket, s3uri.getKey).getObjectContent()
      case _ =>
        throw new IllegalArgumentException(s"Resource at $uri is not valid")
    }
  }

  def readFile(fileUri: String): String = {
    val is = getStream(fileUri)
    try {
      IOUtils.toString(is)
    } finally {
      is.close()
    }
  }
}
