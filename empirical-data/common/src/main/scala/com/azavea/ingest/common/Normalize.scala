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
import com.amazonaws.services.s3.model.ListObjectsRequest

import java.util.HashMap
import scala.collection.JavaConversions._
import scala.collection.mutable

object NormalizeRDD {
  def normalizeFeatureName(rdd: RDD[SimpleFeature], typeName: String)(implicit sc: SparkContext) =
    rdd.mapPartitions({ featureIter =>
      val bufferedFeatures = featureIter.buffered
      val headFeature = bufferedFeatures.head

      val builder = new SimpleFeatureTypeBuilder
      builder.setName(typeName)
      builder.addAll(headFeature.getType.getAttributeDescriptors)
      headFeature.getUserData.map({ case (key, value) => builder.userData(key, value) })
      val sft = builder.buildFeatureType()

      bufferedFeatures.map({ feature =>
        SimpleFeatureBuilder.retype(feature, sft)
      })
    })
}
