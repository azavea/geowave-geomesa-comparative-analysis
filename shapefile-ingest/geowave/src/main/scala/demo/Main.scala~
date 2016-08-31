package demo

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
  case class Params (instanceId: String = "geomesa",
                     zookeepers: String = "zookeeper",
                     user: String = "root",
                     password: String = "GisPwd",
                     tableName: String = "",
                     urlList: Seq[String] = Nil) {

    def convertToJMap(): HashMap[String, String] = {
      val result = new HashMap[String, String]
      result.put("instanceId", instanceId)
      result.put("zookeepers", zookeepers)
      result.put("user", user)
      result.put("password", password)
      //result.put("auths", "")
      //result.put("visibilities", "")
      result.put("tableName", tableName)
      result
    }

  }

  val parser = new scopt.OptionParser[Params]("spark-gm-ingest") {
    head("spark-gm-ingest", "0.1")
    
    opt[String]('i',"instance")
      .action( (s, conf) => conf.copy(instanceId = s) )
      .text("Accumulo instance ID [default=geomesa]")
    opt[String]('z',"zookeepers")
      .action( (s, conf) => conf.copy(zookeepers = s) )
      .text("Zookeepers [default=zookeeper]")
    opt[String]('u',"user")
      .action( (s, conf) => conf.copy(user = s) )
      .text("User namer [default=root]")
    opt[String]('p',"password")
      .action( (s, conf) => conf.copy(password = s) )
      .text("Password [default=GisPwd]")
    opt[String]('t',"table")
      .action( (s, conf) => conf.copy(tableName = s) )
      .required
      .text("Table name")
    help("help").text("Print this usage text")
    arg[String]("<url> ...")
      .unbounded
      .action( (f, conf) => conf.copy(urlList = conf.urlList :+ f) )
      .text("List of .shp file URLs to ingest")
  }

  def ingestShapefileFromURL(params: Params)(url: String) = {
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
    val params = parser.parse(args, Params()) match {
      case Some(prms) => prms
      case None => {
        java.lang.System.exit(0)
        Params()
      }
    }
    println(params.convertToJMap)
    
    // Setup Spark environment
    val sparkConf = (new SparkConf).setAppName("GeoMesa shapefile ingest")
    val sc = new SparkContext(sparkConf)
    println("SparkContext created!")

    val urlRdd = sc.parallelize(params.urlList)

    // Load shapefiles
    urlRdd.foreach (ingestShapefileFromURL(params))
  }
}
