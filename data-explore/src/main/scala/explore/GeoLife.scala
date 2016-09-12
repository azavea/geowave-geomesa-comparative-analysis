package explore

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

  // fs.getCount(Query.ALL)
  // fs.getCount(new Query("trajectory"))


  val ds = DataStoreFinder.getDataStore(params)
  val fs = ds.getFeatureSource("trajectory")

  val beijing = Extent(115.41720860000002, 39.4424669, 117.50711570000001, 41.0586089)
  val inBeijing = s"BBOX(the_geom, ${beijing.xmin},${beijing.ymin},${beijing.xmax},${beijing.ymax})"
  val notInBeijing = s"DISJOINT(the_geom, ${beijing.toPolygon.toWKT})"

  def query(cql: String) = {
    val q = new Query("trajectory", CQL.toFilter(cql))
    q.getHints.put(EXACT_COUNT,true)
    q
  }

  def queryAll = {
    val q = new Query("trajectory")
    q.getHints.put(EXACT_COUNT,true)
    q
  }

  val all = fs.getCount(queryAll)
  val in = fs.getCount(query(inBeijing))
  val out = fs.getCount(query(notInBeijing))

  println(s"         All: $all")
  println(s"  In Beijing:  $in")
  println(s" !In Beijing:  $out")
  println(s" ...missing ${all - (in + out)}?")

  // Getting -
  //         All: 24875845
  //  In Beijing:  7018552
  // !In Beijing:  4115657
  // ...missing 13741636?
}
