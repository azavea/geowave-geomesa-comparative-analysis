package com.azavea.ca.explore

import com.azavea.ca.core.Beijing

import geotrellis.vector._
import geotrellis.vector.io._

import org.locationtech.geomesa.accumulo.data.AccumuloDataStoreParams._
import org.locationtech.geomesa.accumulo.data.{AccumuloDataStore, AccumuloDataStoreFactory}
import org.geotools.data._
import org.apache.spark.{SparkConf, SparkContext}
import org.locationtech.geomesa.compute.spark.GeoMesaSpark
import scala.collection.JavaConversions._
import org.apache.spark.rdd.RDD
import org.opengis.feature.simple.SimpleFeature
import org.apache.hadoop.conf.Configuration
import org.geotools.data._
import org.opengis.filter._
import org.apache.spark.serializer.KryoRegistrator
import org.locationtech.geomesa.features.kryo.serialization.SimpleFeatureSerializer
import org.geotools.feature.collection.AbstractFeatureVisitor
import com.vividsolutions.jts.geom.{Geometry, Point}
import org.locationtech.geomesa.utils.geotools.{SftBuilder, SimpleFeatureTypes}
import org.locationtech.geomesa.features.ScalaSimpleFeatureFactory
import org.locationtech.geomesa.features.ScalaSimpleFeature
import org.locationtech.geomesa.compute.spark.GeoMesaSparkKryoRegistrator
import org.geotools.feature.simple.SimpleFeatureBuilder
import org.locationtech.geomesa.features.ScalaSimpleFeature
import org.geotools.filter.text.ecql.ECQL
import org.geotools.filter.text.cql2.CQL

import org.locationtech.geomesa.accumulo.index.QueryHints.EXACT_COUNT

object Geolife {
  val params = Map(
    "instanceId" -> "gis",
    "zookeepers" -> "ec2-54-198-233-95.compute-1.amazonaws.com",
    "user"       -> "root",
    "password"   -> "secret",
    "tableName"  -> "geomesa.test"
  )

  val ds = DataStoreFinder.getDataStore(params)
  val fs = ds.getFeatureSource("trajectory")

  def query(cql: String) = {
    val q = new Query("trajectory", ECQL.toFilter(cql))
    q.getHints.put(EXACT_COUNT,true)
    q
  }

  def queryAll = {
    val q = new Query("trajectory")
    q.getHints.put(EXACT_COUNT,true)
    q
  }

  def run() = {
    val all = fs.getCount(queryAll)

    val bbIn = fs.getCount(query(Beijing.CQL.inBoundingBox))
    val bbOut = fs.getCount(query(Beijing.CQL.notInBoundingBox))

    val mpIn = fs.getCount(query(Beijing.CQL.inBeijing))
    val mpOut = fs.getCount(query(Beijing.CQL.notInBeijing))

    println(s"         All: $all")
    println(s"  In Beijing BBOX:  $bbIn")
    println(s" !In Beijing BBOX:  $bbOut")
    println(s"        (missing): ${all - (bbIn + bbOut)}?")
    println(s"  In Beijing:  $mpIn")
    println(s" !In Beijing:  $mpOut")
    println(s"        (missing): ${all - (mpIn + mpOut)}?")

    (all, bbIn, bbOut, mpIn, mpOut)
  }

  // Getting -
  //         All: 24875845
  //  In Beijing:  7018552
  // !In Beijing:  4115657
  // ...missing 13741636?
}
