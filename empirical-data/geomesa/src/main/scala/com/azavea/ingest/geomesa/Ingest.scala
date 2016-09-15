 package com.azavea.ingest.geomesa

import com.azavea.ingest.common._

import com.typesafe.scalalogging.Logger
import org.apache.hadoop.mapreduce.Job;
import org.apache.spark.rdd._
import org.apache.spark.{SparkConf, SparkContext}
import org.geotools.data.simple.SimpleFeatureStore
import org.opengis.feature.simple._
import org.geotools.feature.simple._
import org.opengis.feature.`type`.Name
import org.geotools.factory.Hints
import org.geotools.filter.identity.FeatureIdImpl
import org.geotools.data.{DataStoreFinder, DataUtilities, FeatureWriter, Transaction}
import org.apache.spark.rdd._
import org.apache.spark._

import org.locationtech.geomesa.jobs.interop.mapreduce.GeoMesaOutputFormat

import geotrellis.vector.Point
import java.util.HashMap
import scala.collection.concurrent._
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

object Ingest {
   trait CSVorSHP
   case object CSV extends CSVorSHP
   case object SHP extends CSVorSHP
   implicit val readsCSVorSHP = scopt.Read.reads[CSVorSHP]({ s: String =>
     s.toLowerCase match {
       case "csv" => CSV
       case "shp" => SHP
       case "shapefile" => SHP
       case _ => throw new IllegalArgumentException("Must choose either CSV or SHP")
     }
   })

   case class Params (csvOrShp: CSVorSHP = CSV,
                      instanceId: String = "geomesa",
                      zookeepers: String = "zookeeper",
                      user: String = "root",
                      password: String = "secret",
                      tableName: String = "",
                      dropLines: Int = 0,
                      separator: String = "\t",
                      codec: CSVSchemaParser.Expr = CSVSchemaParser.Spec(Nil),
                      featureName: String = "default-feature-name",
                      s3bucket: String = "",
                      s3prefix: String = "",
                      csvExtension: String = ".csv",
                      translationPoints: Seq[Point] = Seq.empty,
                      translationOrigin: Option[Point] = None,
                      unifySFT: Boolean = true) {

     def convertToJMap(): HashMap[String, String] = {
       val result = new HashMap[String, String]
       result.put("instanceId", instanceId)
       result.put("zookeepers", zookeepers)
       result.put("user", user)
       result.put("password", password)
       result.put("tableName", tableName)
       result
     }
   }

   def registerSFT(params: Params)(sft: SimpleFeatureType) = {
     val ds = DataStoreFinder.getDataStore(params.convertToJMap)

     if (ds == null) {
       println("Could not build AccumuloDataStore")
       java.lang.System.exit(-1)
     }

     ds.createSchema(sft)
     ds.dispose
   }

   def ingestRDD(params: Params)(rdd: RDD[SimpleFeature]) = {
     val conf = rdd.sparkContext.hadoopConfiguration
     val job = new Job(conf, "ingest job")

     job.setOutputFormatClass(classOf[GeoMesaOutputFormat]);
     job.setMapOutputKeyClass(classOf[org.apache.hadoop.io.Text]);
     job.setMapOutputValueClass(classOf[SimpleFeature]);
     job.setNumReduceTasks(0)

     GeoMesaOutputFormat.configureDataStore(job, params.convertToJMap)

     rdd
       .map { z => (new org.apache.hadoop.io.Text, z) }
       .saveAsNewAPIHadoopDataset(job.getConfiguration)
   }

   /** The method for ingest here is based on:
    * https://github.com/locationtech/geomesa/blob/master/geomesa-tools/src/main/scala/org/locationtech/geomesa/tools/accumulo/ingest/AbstractIngest.scala#L104
   **/
   def ingestRDD2(params: Params)(rdd: RDD[SimpleFeature]): Unit =
     rdd.foreachPartition { featureIter =>
       val ds = DataStoreFinder.getDataStore(params.convertToJMap)

       if (ds == null) {
         println("Could not build AccumuloDataStore")
         java.lang.System.exit(-1)
       }

       var registered = TrieMap.empty[String, FeatureWriter[SimpleFeatureType, SimpleFeature]]

       try {
         featureIter.foreach { feature: SimpleFeature =>
           val sft = feature.getType
           val fw = registered.getOrElseUpdate(sft.getTypeName, {
                                                 ds.createSchema(sft) // register every new schema type
                                                 ds.getFeatureWriterAppend(sft.getTypeName, Transaction.AUTO_COMMIT)
                                               })

           val toWrite = fw.next()
           toWrite.setAttributes(feature.getAttributes)
           toWrite.getIdentifier.asInstanceOf[FeatureIdImpl].setID(feature.getID)
           toWrite.getUserData.putAll(feature.getUserData)
           toWrite.getUserData.put(Hints.USE_PROVIDED_FID, java.lang.Boolean.TRUE)

           try {
             fw.write()
           } catch {
             case e: Exception =>
               println(s"Failed to write a feature", feature, e)
               throw e
           }
         }
       }
       catch {
         case e: Exception =>
           println(e)
           throw e
       }
       finally {
         for ((_, fw) <- registered) fw.close()
         if (ds != null) ds.dispose()
       }
     }
}
