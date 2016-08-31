package demo

//import com.typesafe.scalalogging.Logger
import mil.nga.giat.geowave.adapter.vector._
import mil.nga.giat.geowave.core.geotime.ingest._
import mil.nga.giat.geowave.core.{store => geowave}
import mil.nga.giat.geowave.core.store.index._
import mil.nga.giat.geowave.datastore.accumulo._
import mil.nga.giat.geowave.datastore.accumulo.index.secondary._
import mil.nga.giat.geowave.datastore.accumulo.metadata._
import mil.nga.giat.geowave.datastore.accumulo.operations.config.AccumuloOptions
import org.apache.spark.{SparkConf, SparkContext}
import org.geotools.data.{DataStoreFinder, FeatureSource}
import org.geotools.data.simple.SimpleFeatureStore
import org.geotools.feature.FeatureCollection
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}
import org.opengis.filter.Filter
//import org.locationtech.geomesa.accumulo.data.AccumuloDataStore
//import org.locationtech.geomesa.utils.geotools.GeneralShapefileIngest

import java.util.HashMap
import scala.collection.JavaConversions._
import scala.util.Try

object Main {
  case class Params (instanceId: String = "geowave",
                     zookeepers: String = "zookeeper",
                     user: String = "root",
                     password: String = "GisPwd",
                     geowaveNamespace: String = "",
                     urlList: Seq[String] = Nil)

  val parser = new scopt.OptionParser[Params]("geowave-shapefile-ingest") {
    head("geowave-shapefile-ingest", "0.1")
    
    opt[String]('i',"instance")
      .action( (s, conf) => conf.copy(instanceId = s) )
      .text("Accumulo instance ID [default=geowave]")
    opt[String]('z',"zookeepers")
      .action( (s, conf) => conf.copy(zookeepers = s) )
      .text("Zookeepers [default=zookeeper]")
    opt[String]('u',"user")
      .action( (s, conf) => conf.copy(user = s) )
      .text("User namer [default=root]")
    opt[String]('p',"password")
      .action( (s, conf) => conf.copy(password = s) )
      .text("Password [default=GisPwd]")
    opt[String]('n',"namespace")
      .action( (s, conf) => conf.copy(geowaveNamespace = s) )
      .required
      .text("Geowave namespace")
    help("help").text("Print this usage text")
    arg[String]("<url> ...")
      .unbounded
      .action( (f, conf) => conf.copy(urlList = conf.urlList :+ f) )
      .text("List of .shp file URLs to ingest")
  }

  def getGeowaveDataStore(conf: Params): AccumuloDataStore = {
    // GeoWave persists both the index and data adapter to the same accumulo
    // namespace as the data. The intent here
    // is that all data is discoverable without configuration/classes stored
    // outside of the accumulo instance.
    val instance = new BasicAccumuloOperations(conf.zookeepers,
                                               conf.instanceId,
                                               conf.user,
                                               conf.password,
                                               conf.geowaveNamespace)

    val options = new AccumuloOptions
    options.setPersistDataStatistics(true)
    //options.setUseAltIndex(true)

    return new AccumuloDataStore(
      new AccumuloIndexStore(instance),
      new AccumuloAdapterStore(instance),
      new AccumuloDataStatisticsStore(instance),
      new AccumuloSecondaryIndexDataStore(instance),
      new AccumuloAdapterIndexMappingStore(instance),
      instance,
      options)
  }

  def ingestShapefileFromURL(params: Params)(url: String) = {
    val shpParams = new HashMap[String, Object]
    shpParams.put("url", url)
    val shpDS = DataStoreFinder.getDataStore(shpParams)
    if (shpDS == null) {
      println("Could not build ShapefileDataStore")
      java.lang.System.exit(-1)
    }

    val typeName = shpDS.getTypeNames()(0)
    val source: FeatureSource[SimpleFeatureType, SimpleFeature] = shpDS.getFeatureSource(typeName)
    val schema = shpDS.getSchema(typeName)
    val collection: FeatureCollection[SimpleFeatureType, SimpleFeature] = source.getFeatures(Filter.INCLUDE)
    val features = collection.features

    val maybeGWDataStore = Try(getGeowaveDataStore(params))
    if (maybeGWDataStore.isFailure) {
      println("Could not connect to Accumulo instance")
      println(maybeGWDataStore)
      java.lang.System.exit(-1)
    }
    val ds = maybeGWDataStore.get

    // Prepare to write to Geowave
    val adapter = new FeatureDataAdapter(schema)
    val index = (new SpatialDimensionalityTypeProvider).createPrimaryIndex
    val indexWriter = ds.createWriter(adapter, index).asInstanceOf[geowave.IndexWriter[SimpleFeature]]

    // Write features from shapefile
    var i = 0
    while (features.hasNext()) {
      val feature = features.next()
      indexWriter.write(feature)
      i += 1
    }
    println(s"$i of ${collection.size} features read")

    // Clean up
    features.close
    indexWriter.close
    shpDS.dispose
  }

  def main(args: Array[String]) = {
    val params = parser.parse(args, Params()) match {
      case Some(prms) => prms
      case None => {
        java.lang.System.exit(0)
        Params()
      }
    }
    
    // Setup Spark environment
    val sparkConf = (new SparkConf).setAppName("GeoWave shapefile ingest")
    val sc = new SparkContext(sparkConf)
    println("SparkContext created!")

    val urlRdd = sc.parallelize(params.urlList)

    // Load shapefiles
    urlRdd.foreach (ingestShapefileFromURL(params))
  }
}
