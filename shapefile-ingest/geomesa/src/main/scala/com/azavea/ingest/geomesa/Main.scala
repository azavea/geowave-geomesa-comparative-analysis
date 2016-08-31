package com.azavea.ingest.geomesa

import com.typesafe.scalalogging.Logger
//import geotrellis.spark.util.SparkUtils
import org.apache.spark.{SparkConf, SparkContext}
import org.geotools.data.DataStoreFinder
import org.geotools.data.simple.SimpleFeatureStore
import org.locationtech.geomesa.accumulo.data.AccumuloDataStore
import org.locationtech.geomesa.utils.geotools.GeneralShapefileIngest

import java.util.HashMap
import scala.collection.JavaConversions._

object Main {


  def ingestShapefileFromURL(params: CommandLine.Params)(url: String) = {
    val shpParams = new HashMap[String, Object]
    shpParams.put("url", url)
    val shpDS = DataStoreFinder.getDataStore(shpParams)
    if (shpDS == null) {
      println("Could not build ShapefileDataStore")
      java.lang.System.exit(-1)
    }

    val iter = DataStoreFinder.getAvailableDataStores
    while (iter.hasNext) {
      val storeType = iter.next
      println(storeType)
    }

    val ds = DataStoreFinder.getDataStore(params.convertToJMap)
    if (ds == null) {
      println("Could not build AccumuloDataStore")
      java.lang.System.exit(-1)
    }

    ds.createSchema(shpDS.getSchema(shpDS.getNames()(0)))

    val featureCollection = shpDS.getFeatureSource(shpDS.getNames()(0)).getFeatures
    val featureStore = ds.getFeatureSource(shpDS.getNames()(0)).asInstanceOf[SimpleFeatureStore]
    featureStore.addFeatures(featureCollection)

    ds.dispose
  }

  def main(args: Array[String]) = {
    val params: CommandLine.Params = CommandLine.parser.parse(args, CommandLine.Params()) match {
      case Some(prms) => prms
      case None => {
        java.lang.System.exit(0)
        CommandLine.Params()
      }
    }
    println(params.convertToJMap)

    // Setup Spark environment
    val sparkConf = (new SparkConf).setAppName("GeoMesa shapefile ingest")
    val sc = new SparkContext(sparkConf)
    println("SparkContext created!")

    val shapefileUrls: Array[String] = ???

    val urlRdd = sc.parallelize(shapefileUrls)

    // Load shapefiles
    urlRdd.foreach (ingestShapefileFromURL(params))
  }
}
