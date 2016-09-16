package com.azavea.ingest.common.avro

import com.azavea.ingest.common._
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
import org.apache.avro._
import org.apache.avro.file._
import org.apache.avro.generic._
import org.apache.avro.io._
import org.apache.avro.Schema
import com.vividsolutions.jts.geom.{LineString}
import com.vividsolutions.jts.io.WKBReader
import org.geotools.referencing.crs.DefaultGeographicCRS
import java.util.HashMap
import java.nio._
import java.io._
import scala.collection.JavaConverters._
import scala.collection.mutable


object AvroRDD {

  def generatedTrackSFT(featureName: String) = {
    val builder = new SimpleFeatureTypeBuilder
    val attr = new AttributeTypeBuilder
    builder.setName(featureName)
    builder.setCRS(DefaultGeographicCRS.WGS84)
    builder.add(attr.binding(classOf[LineString]).nillable(false).buildDescriptor("the_geom"))
    builder.add(attr.binding(classOf[Integer]).nillable(false).buildDescriptor("numTrackPoints"))
    builder.add(attr.binding(classOf[java.util.Date]).nillable(false).buildDescriptor("TimeStamp"))
    builder.add(attr.binding(classOf[String]).nillable(false).buildDescriptor("key"))
    builder.add(attr.binding(classOf[java.lang.Double]).nillable(false).buildDescriptor("totalDistance"))
    builder.buildFeatureType
  }

  def convertToSimpleFeature(record: GenericRecord, sft: SimpleFeatureType) = {
    val simpleFeatureCollection = record.get(1).asInstanceOf[GenericData.Array[GenericRecord]].get(0)
    require(record.get(1).asInstanceOf[GenericData.Array[GenericRecord]].size == 1)
    val values = simpleFeatureCollection.get("values").asInstanceOf[GenericData.Array[ByteBuffer]]
    val geometry = values.get(0).array
    val timeStamp = values.get(1).array // serialized java.util.Date
    val key = values.get(2).array // String
    val totalDistance = values.get(3).array // java.lang.Double
    val numTrackPoints = values.get(4).array // Integer

    val linestring = (new WKBReader).read(geometry).asInstanceOf[LineString]

    val buf: ByteBuffer = ByteBuffer.allocate(8)
    val millis: java.lang.Long = buf.put(timeStamp).flip.asInstanceOf[ByteBuffer].getLong
    val trackDate = new java.util.Date(millis)

    val keyString = key.map(_.toChar).mkString

    val pathLength: java.lang.Double = buf.flip.asInstanceOf[ByteBuffer].put(totalDistance).flip.asInstanceOf[ByteBuffer].getDouble

    val buf4: ByteBuffer = ByteBuffer.allocate(4)
    val numPts: java.lang.Integer = buf4.put(numTrackPoints).flip.asInstanceOf[ByteBuffer].getInt

    val fid = simpleFeatureCollection.get("fid").asInstanceOf[String]
    val cls = simpleFeatureCollection.get("classifications").asInstanceOf[String]
    require(cls == null, "classifications not null")

    if (linestring.getNumPoints != numPts)
      println(s"LineString point number disagreement")

    val builder = new SimpleFeatureBuilder(sft)

    builder.set("the_geom", linestring)
    builder.set("numTrackPoints", numPts)
    builder.set("TimeStamp", trackDate)
    builder.set("key", keyString)
    builder.set("totalDistance", pathLength)

    val result = builder.buildFeature(fid)

    if (((result.getAttribute("the_geom")).asInstanceOf[LineString]).getNumPoints != numPts)
      println(s"SimpleFeature geometry point number disagreement")

    result
  }

  def apply(featureName: String, urls: Seq[String], partitionSize: Int = 10)(implicit sc: SparkContext): RDD[SimpleFeature] = {
    sc.parallelize(urls, urls.size / partitionSize)
      .flatMap { url =>
        println(s"Read: $url")
        val is: InputStream = Util.getStream(url)
        val sft = generatedTrackSFT(featureName)
        val datumReader = new GenericDatumReader[GenericRecord](schema(schemaString))
        val dfr = new DataFileStream[GenericRecord](is, datumReader)
        val iter: Iterator[SimpleFeature] = dfr.iterator.asScala.map { rec: GenericRecord =>
          convertToSimpleFeature(rec, sft)
        }
        iter ++ { dfr.close(); Iterator.empty }
      }
  }

  def schema(str: String): Schema = {
    (new Schema.Parser).parse(str)
  }

  val schemaString =
"""
{
  "type" : "record",
  "name" : "AvroSimpleFeatureCollection",
  "namespace" : "mil.nga.giat.geowave.adapter.vector.avro",
  "fields" : [ {
    "name" : "featureType",
    "type" : {
      "type" : "record",
      "name" : "FeatureDefinition",
      "fields" : [ {
        "name" : "featureTypeName",
        "type" : {
          "type" : "string",
          "avro.java.string" : "String"
        }
      }, {
        "name" : "attributeNames",
        "type" : {
          "type" : "array",
          "items" : {
            "type" : "string",
            "avro.java.string" : "String"
          }
        }
      }, {
        "name" : "attributeTypes",
        "type" : {
          "type" : "array",
          "items" : {
            "type" : "string",
            "avro.java.string" : "String"
          }
        }
      }, {
        "name" : "attributeDefaultClassifications",
        "type" : {
          "type" : "array",
          "items" : {
            "type" : "string",
            "avro.java.string" : "String"
          }
        }
      } ]
    }
  }, {
    "name" : "simpleFeatureCollection",
    "type" : {
      "type" : "array",
      "items" : {
        "type" : "record",
        "name" : "AttributeValues",
        "fields" : [ {
          "name" : "fid",
          "type" : {
            "type" : "string",
            "avro.java.string" : "String"
          }
        }, {
          "name" : "values",
          "type" : {
            "type" : "array",
            "items" : "bytes"
          }
        }, {
          "name" : "classifications",
          "type" : [ "null", {
            "type" : "array",
            "items" : {
              "type" : "string",
              "avro.java.string" : "String"
            }
          } ]
        } ]
      }
    }
  } ]
}
"""
}
