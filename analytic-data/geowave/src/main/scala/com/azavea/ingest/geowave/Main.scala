package com.azavea.ingest.geowave

//import com.typesafe.scalalogging.Logger
import mil.nga.giat.geowave.adapter.vector._
import mil.nga.giat.geowave.core.geotime.ingest._
import mil.nga.giat.geowave.core.{store => geowave}
import mil.nga.giat.geowave.core.store.index._
import mil.nga.giat.geowave.datastore.accumulo._
import mil.nga.giat.geowave.datastore.accumulo.index.secondary._
import mil.nga.giat.geowave.datastore.accumulo.metadata._
import mil.nga.giat.geowave.datastore.accumulo.operations.config.AccumuloOptions
import org.apache.spark.{SparkConf, SparkContext}
import org.geotools.data.{DataStoreFinder, FeatureSource}
import org.geotools.data.simple.SimpleFeatureStore
import org.geotools.feature.FeatureCollection
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}
import org.opengis.filter.Filter
//import org.locationtech.geomesa.accumulo.data.AccumuloDataStore
//import org.locationtech.geomesa.utils.geotools.GeneralShapefileIngest

import java.util.HashMap
import scala.collection.JavaConversions._
import scala.util.Try

object Main {
  def main(args: Array[String]) = {
    val params = parser.parse(args, Params()) match {
      case Some(p) => p
      case None => {
        java.lang.System.exit(0)
        Params()
      }
    }

    // Setup Spark environment
    val sparkConf = (new SparkConf).setAppName("GeoWave shapefile ingest")
    val sc = new SparkContext(sparkConf)
    println("SparkContext created!")

    val urlRdd = sc.parallelize(params.urlList)

    // Load shapefiles
    urlRdd.foreach (ingestShapefileFromURL(params))
  }
}
