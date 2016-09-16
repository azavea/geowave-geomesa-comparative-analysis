package com.azavea.ingest.common.shp

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
import org.opengis.feature.`type`.Name
import java.util.HashMap
import scala.collection.JavaConverters._
import scala.collection.mutable

import com.azavea.ingest.common._

class IteratorWrapper[I, T](iter: I)(hasNext: I => Boolean, next: I => T, close: I => Unit) extends Iterator[T] {
  def hasNext = {
    val has = hasNext(iter)
    if (! has) close(iter)
    has
  }
  def next = next(iter)
}

object HydrateRDD {

  def getShpUrls(s3bucket: String, s3prefix: String, recursive: Boolean = false): Array[String] =
    Util.listKeys(s3bucket, s3prefix, ".shp", recursive)

  def shpUrlsToRdd(urlArray: Array[String], partitionSize: Int = 10)(implicit sc: SparkContext): RDD[SimpleFeature] = {
    val urlRdd: RDD[String] = sc.parallelize(urlArray, urlArray.size / partitionSize)
    urlRdd.mapPartitions { urls =>
      urls.flatMap { url =>
        val datastoreParams = Map("url" -> url).asJava
        val shpDS = DataStoreFinder.getDataStore(datastoreParams)
        require(shpDS != null, "Could not build ShapefileDataStore")

        shpDS.getNames.asScala.flatMap { name: Name =>
          val features =
            shpDS
            .getFeatureSource(name)
            .getFeatures
            .features
          new IteratorWrapper(features)(_.hasNext, _.next, _.close)
        }
      }
    }
  }
}
