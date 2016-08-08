package com.azavea.geowave

import com.azavea.common._

import mil.nga.giat.geowave.adapter.vector.FeatureDataAdapter
import mil.nga.giat.geowave.core.geotime.ingest._
import mil.nga.giat.geowave.core.store.{ DataStore, IndexWriter }
import mil.nga.giat.geowave.datastore.accumulo._
import org.apache.spark.{SparkConf, SparkContext}
import org.locationtech.geomesa.utils.geotools.SimpleFeatureTypes // sic!
import org.opengis.feature.simple.SimpleFeature


object WavePoke extends CommonPoke {

  def main(args: Array[String]): Unit = {

    if (args.length < 6) {
      println(s"arguments: <instance> <zookeepers> <user> <password> <namespace> <instruction\{1,\}>")
      System.exit(-1)
    }

    // Spark Context
    val sparkConf = (new SparkConf).setAppName("GeoWave Synthetic Data Ingest")
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
    val geometries = args.drop(5).toList.flatMap(decode)

    // Store Geometries in GeoWave
    sparkContext
      .parallelize(geometries, geometries.length)
      .foreach({ tuple =>
        // Create an AccumuloDataStore
        val basicOpsInstance = new BasicAccumuloOperations(args(1), args(0), args(2), args(3), args(4))
        val ds = new AccumuloDataStore(basicOpsInstance)

        // Create Index
        val index = (new SpatialTemporalDimensionalityTypeProvider.SpatialTemporalIndexBuilder).createIndex()

        val (name, spec) = sftMap.value.getOrElse(tuple._1, throw new Exception)
        val sft = SimpleFeatureTypes.createType(name, spec)
        val adapter = new FeatureDataAdapter(sft)
        val indexWriter = ds.createWriter(adapter, index).asInstanceOf[IndexWriter[SimpleFeature]]
        val itr = tuple match {
          case (_: String, n: Int, -1.0, randomTime: Boolean, seed: Long) =>
            PointGenerator(n, sft, randomTime, seed).features
          case (_: String, n: Int, meters: Double, randomTime: Boolean, seed: Long) =>
            ExtentGenerator(n, meters, sft, randomTime, seed).features
        }

        while (itr.hasNext) { indexWriter.write(itr.next) }
        itr.close
        indexWriter.close
      })

    sparkContext.stop

  }

}
