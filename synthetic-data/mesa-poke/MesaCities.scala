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
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}
import org.apache.spark.rdd._
import spray.json._
import spray.json.DefaultJsonProtocol._
import geotrellis.vector._
import geotrellis.vector.io._
import geotrellis.vector.io.json._


/**
  * Here we're going to avoid avoid specifying the centers of features by using centers of 661 cities.
  * Other parameters are still expected and will be used to generate the commands as in fully synthetic case.
  */
object MesaCities extends CommonPoke {

  def main(args: Array[String]): Unit = {
    val conf = CommandLine.parser.parse(args, CommandLine.DEFAULT_OPTIONS).get

    val eitherSft = MesaPoke.eitherSft
    val extentSft = MesaPoke.extentSft
    val pointSft = MesaPoke.pointSft

    // Register types with GeoMesa
    val ds = DataStoreFinder.getDataStore(conf.dataSourceConf).asInstanceOf[AccumuloDataStore]
    conf.instructions
      .map({ inst => inst.split(",").head }).distinct
      .map({ kind =>
             kind match {
               case `either` => eitherSft
               case `extent` => extentSft
               case `point` => pointSft
               case str =>
                 throw new Exception(str)
             }
           })
      .foreach({ sft => ds.createSchema(sft) })

    // Spark Context
    val sparkConf = (new SparkConf).setAppName("GeoMesa Synthetic Cities Ingest")
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

    val geometries = scala.util.Random.shuffle(Cities.geometries(conf.instructions))

    // Store Geometries in GeoMesa
    val rdd: RDD[SimpleFeature] =
      sparkContext.parallelize(geometries, geometries.length)
        .flatMap { tuple =>
          val (name, spec) = sftMap.value.getOrElse(tuple._1, throw new Exception)
          val schema = SimpleFeatureTypes.createType(name, spec)
          tuple match {
            case (_, seed: Long, lng: String, lat: String, time: String, width: String) =>
              GeometryGenerator(schema, seed, lng, lat, time, width)
          }
        }

    MesaPoke.ingestRDDWithOutputFormat(conf.dataSourceConf, rdd)
    sparkContext.stop
  }
}
