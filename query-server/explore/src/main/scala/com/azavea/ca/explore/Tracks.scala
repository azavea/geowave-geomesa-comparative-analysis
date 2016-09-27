package com.azavea.ca.explore


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


object Tracks {
  val params = Map(
    "instanceId" -> "gis",
    "zookeepers" -> "localhost",
    "user"       -> "root",
    "password"   -> "secret",
    "tableName"  -> "geomesa.tracks"
  )

  val ds = DataStoreFinder.getDataStore(params)
  val fs = ds.getFeatureSource("generated-tracks")

  def query(cql: String) = {
    val q = new Query("generated-tracks", ECQL.toFilter(cql))
    q.getHints.put(EXACT_COUNT,true)
    q
  }

  def queryAll = {
    val q = new Query("generated-tracks")
    q.getHints.put(EXACT_COUNT,true)
    q
  }
}
