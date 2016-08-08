package com.azavea.geomesa

import com.azavea.common._

import org.apache.spark.{SparkConf, SparkContext}
import org.geotools.data._
import org.geotools.data.simple.SimpleFeatureStore
import org.geotools.factory.Hints
import org.geotools.feature._
import org.locationtech.geomesa.accumulo.data.AccumuloDataStore
import org.locationtech.geomesa.accumulo.index.Constants
import org.locationtech.geomesa.utils.geotools.SimpleFeatureTypes


object MesaPoke extends CommonPoke {

  def main(args: Array[String]): Unit = {

    if (args.length < 6) {
      println(s"arguments: <instance> <zookeepers> <user> <password> <table> <instruction\{1,\}>")
      System.exit(-1)
    }

    val dsConf = new java.util.HashMap[String, String]()
    dsConf.put("instanceId", args(0))
    dsConf.put("zookeepers", args(1))
    dsConf.put("user", args(2))
    dsConf.put("password", args(3))
    dsConf.put("tableName", args(4))

    eitherSft.getUserData.put(Constants.SF_PROPERTY_START_TIME, CommonSimpleFeatureType.whenField) // Inform GeoMesa which field contains "time"
    eitherSft.getUserData.put("geomesa.mixed.geometries", java.lang.Boolean.TRUE) // Allow GeoMesa to index points and extents together
    extentSft.getUserData.put(Constants.SF_PROPERTY_START_TIME, CommonSimpleFeatureType.whenField)
    pointSft.getUserData.put(Constants.SF_PROPERTY_START_TIME, CommonSimpleFeatureType.whenField)

    // Register types with GeoMesa
    val ds = DataStoreFinder.getDataStore(dsConf).asInstanceOf[AccumuloDataStore]
    args.drop(5)
      .map({ inst => inst.split(":").head }).distinct
      .map({ kind =>
        kind match {
          case `either` => eitherSft
          case `extent` => extentSft
          case `point` => pointSft
        }
      })
      .foreach({ sft => ds.createSchema(sft) })

    // Spark Context
    val sparkConf = (new SparkConf).setAppName("GeoMesa Synthetic Data Ingest")
    val sparkContext = new SparkContext(sparkConf)

    // Create a map of encoded SimpleFeatureTypes.  This map can cross
    // serialization boundary.
    val sftMap = sparkContext.broadcast(
      Map(
        either -> (eitherSft.getTypeName, SimpleFeatureTypes.encodeType(eitherSft)),
        extent -> (extentSft.getTypeName, SimpleFeatureTypes.encodeType(extentSft)),
        point -> (pointSft.getTypeName, SimpleFeatureTypes.encodeType(pointSft))
      )
    )

    // Generate List of Geometries
    val geometries = args.drop(5).toList.flatMap(decode)

    // Store Geometries in GeoMesa
    sparkContext
      .parallelize(geometries, geometries.length)
      .foreach({ tuple =>
        val (name, spec) = sftMap.value.getOrElse(tuple._1, throw new Exception)
        val sft = SimpleFeatureTypes.createType(name, spec)
        val fc =
          tuple match {
            case (_: String, n: Int, -1.0, randomTime: Boolean, seed: Long) =>
              PointGenerator(n, sft, randomTime, seed)
            case (_: String, n: Int, meters: Double, randomTime: Boolean, seed: Long) =>
              ExtentGenerator(n, meters, sft, randomTime, seed)
          }
        val ds = DataStoreFinder.getDataStore(dsConf)

        ds.getFeatureSource(sft.getTypeName).asInstanceOf[SimpleFeatureStore].addFeatures(fc)
        ds.dispose
      })

    sparkContext.stop
  }

}
