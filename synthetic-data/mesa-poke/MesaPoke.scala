package com.azavea.geomesa

import com.azavea.common._

import org.apache.spark.{SparkConf, SparkContext}
import org.geotools.data._
import org.geotools.data.simple.SimpleFeatureStore
import org.geotools.factory.Hints
import org.geotools.feature._
import org.locationtech.geomesa.accumulo.data.AccumuloDataStore
import org.locationtech.geomesa.jobs.interop.mapreduce.GeoMesaOutputFormat
import org.locationtech.geomesa.accumulo.index.Constants
import org.locationtech.geomesa.utils.geotools.SimpleFeatureTypes
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}
import org.apache.spark.rdd._
import org.apache.hadoop.mapreduce.Job;

object MesaPoke extends CommonPoke {

  def main(args: Array[String]): Unit = {
    val conf = CommandLine.parser.parse(args, CommandLine.DEFAULT_OPTIONS).get

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
    val geometries = conf.instructions.flatMap(decode)

    // Store Geometries in GeoMesa
    sparkContext
      .parallelize(geometries, geometries.length)
      .foreach({ tuple =>
                 val (name, spec) = sftMap.value.getOrElse(tuple._1, throw new Exception)
                 val schema = SimpleFeatureTypes.createType(name, spec)
                 val iter =
                   tuple match {
                     case (_, seed: Long, lng: String, lat: String, time: String, width: String) =>
                       GeometryGenerator(schema, seed, lng, lat, time, width)
                   }
                 val ds = DataStoreFinder.getDataStore(conf.dataSourceConf)
                 MesaPoke.writeFeatures(schema.getTypeName, ds, iter)
                 ds.dispose
               })

    sparkContext.stop
  }

  val eitherSft = {
    val sft = CommonSimpleFeatureType("Geometry")
    sft.getUserData.put(Constants.SF_PROPERTY_START_TIME, CommonSimpleFeatureType.whenField) // Inform GeoMesa which field contains "time"
    sft.getUserData.put("geomesa.mixed.geometries", java.lang.Boolean.TRUE) // Allow GeoMesa to index points and extents together
    sft
  }

  val extentSft = {
    val sft = CommonSimpleFeatureType("Polygon")
    sft.getUserData.put(Constants.SF_PROPERTY_START_TIME, CommonSimpleFeatureType.whenField)
    sft
  }

  val pointSft = {
    val sft = CommonSimpleFeatureType("Point")
    sft.getUserData.put(Constants.SF_PROPERTY_START_TIME, CommonSimpleFeatureType.whenField)
    sft
  }

  def writeFeatures(typeName: String, ds: DataStore, iter: Iterator[SimpleFeature]): Unit = {
    val fw = ds.getFeatureWriterAppend(typeName, Transaction.AUTO_COMMIT)

    for ( feature <- iter ) {
      val toWrite = fw.next()
      toWrite.setAttributes(feature.getAttributes)
      toWrite.getUserData.putAll(feature.getUserData)
      fw.write()
    }
    fw.close()
  }

  def ingestRDDWithOutputFormat(params: java.util.HashMap[String,String], rdd: RDD[SimpleFeature]) = {
    val conf = rdd.sparkContext.hadoopConfiguration
    val job = new Job(conf, "ingest job")

    job.setOutputFormatClass(classOf[GeoMesaOutputFormat]);
    job.setMapOutputKeyClass(classOf[org.apache.hadoop.io.Text]);
    job.setMapOutputValueClass(classOf[SimpleFeature]);
    job.setNumReduceTasks(0)


    GeoMesaOutputFormat.configureDataStore(job, params)

    rdd
      .map { z => (new org.apache.hadoop.io.Text, z) }
      .saveAsNewAPIHadoopDataset(job.getConfiguration)
  }
}
