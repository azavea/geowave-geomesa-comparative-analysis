package com.azavea.ingest.geomesa

import com.typesafe.scalalogging.Logger
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.geotools.data.simple.SimpleFeatureStore
import org.opengis.feature.simple._
import org.geotools.feature.simple._
import org.opengis.feature.`type`.Name
import org.geotools.factory.Hints
import org.geotools.filter.identity.FeatureIdImpl
import org.geotools.data.{DataStoreFinder, DataUtilities, FeatureWriter, Transaction}

import java.util.HashMap
import scala.collection.JavaConversions._

import com.azavea.ingest.common._

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
                     unifySFT: Boolean = true) {

    def convertToJMap(): HashMap[String, String] = {
      val result = new HashMap[String, String]
      result.put("instanceId", instanceId)
      result.put("zookeepers", zookeepers)
      result.put("user", user)
      result.put("password", password)
      result.put("tableName", tableName)
      println(result)
      result
    }
  }

  def registerSFT(params: Params)(sft: SimpleFeatureType) = {
    println("registering sft")
    val ds = DataStoreFinder.getDataStore(params.convertToJMap)

    if (ds == null) {
      println("Could not build AccumuloDataStore")
      java.lang.System.exit(-1)
    }

    println("SFT", sft)
    ds.createSchema(sft)
    ds.dispose
  }

  def ingestRDD(params: Params)(rdd: RDD[SimpleFeature]) =
    /* The method for ingest here is based on:
     * https://github.com/locationtech/geomesa/blob/master/geomesa-tools/src/main/scala/org/locationtech/geomesa/tools/accumulo/ingest/AbstractIngest.scala#L104
     */

    rdd.foreachPartition({ featureIter =>
      val ds = DataStoreFinder.getDataStore(params.convertToJMap)

      if (ds == null) {
        println("Could not build AccumuloDataStore")
        java.lang.System.exit(-1)
      }

      var fw: FeatureWriter[SimpleFeatureType, SimpleFeature] = null
      try {
        fw = ds.getFeatureWriterAppend(params.featureName, Transaction.AUTO_COMMIT)
        featureIter.toStream.foreach({ feature: SimpleFeature =>
            println(feature.toString)
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
        })
      } finally {
        fw.close()
        ds.dispose()
      }
    })
}
