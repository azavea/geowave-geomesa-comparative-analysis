package com.azavea.ca.planning

import com.vividsolutions.jts.geom._
import mil.nga.giat.geowave.adapter.vector._
import mil.nga.giat.geowave.adapter.vector.query.cql.CQLQuery
import mil.nga.giat.geowave.core.geotime.index.dimension._
import mil.nga.giat.geowave.core.geotime.index.dimension.TemporalBinningStrategy.{ Unit => BinUnit }
import mil.nga.giat.geowave.core.geotime.ingest._
import mil.nga.giat.geowave.core.geotime.store.query._
import mil.nga.giat.geowave.core.index.dimension.NumericDimensionDefinition
import mil.nga.giat.geowave.core.index.IndexMetaData
import mil.nga.giat.geowave.core.index.sfc.SFCDimensionDefinition
import mil.nga.giat.geowave.core.index.sfc.SFCFactory.SFCType
import mil.nga.giat.geowave.core.index.sfc.tiered.TieredSFCIndexFactory
import mil.nga.giat.geowave.core.store.DataStore
import mil.nga.giat.geowave.core.store.index._
import mil.nga.giat.geowave.core.store.index.writer.IndexWriter
import mil.nga.giat.geowave.datastore.accumulo._
import mil.nga.giat.geowave.datastore.accumulo.index.secondary._
import mil.nga.giat.geowave.datastore.accumulo.metadata._
import mil.nga.giat.geowave.datastore.accumulo.operations.config.AccumuloOptions
import org.geotools.feature.simple.SimpleFeatureTypeBuilder
import org.geotools.referencing.CRS
import org.opengis.feature.simple.SimpleFeatureType


object WavePlan {

  val rng = new scala.util.Random

  // The current default
  val MAX_RANGE_DECOMPOSITION = 5000

  // For CQL queries
  val authorityFactory = CRS.getAuthorityFactory(true)
  val epsg4326 = authorityFactory.createCoordinateReferenceSystem("EPSG:4326")
  val sftb = (new SimpleFeatureTypeBuilder).minOccurs(1).maxOccurs(1).nillable(false)
  val sftName = "CommonSimpleFeatureType"
  sftb.setName(sftName)
  sftb.add("when", classOf[java.util.Date])
  sftb.setCRS(epsg4326)
  sftb.add("where", classOf[Point])
  val sft = sftb.buildFeatureType
  val adapter = new FeatureDataAdapter(sft)

  // For "regular" queries
  val geometryFactory = new GeometryFactory

  /*******************
   * CREATE STRATEGY *
   *******************/
  def createStrategy(period: String) = {
    val DIMENSIONS = period match {
      case "day" | "week" | "year" =>
        val dim1 = new SFCDimensionDefinition(new LongitudeDefinition, 21)
        val dim2 = new SFCDimensionDefinition(new LatitudeDefinition(true), 21)
        val dim3 = period match {
          case "day" => new SFCDimensionDefinition(new TimeDefinition(BinUnit.DAY), 20)
          case "week" => new SFCDimensionDefinition(new TimeDefinition(BinUnit.WEEK), 20)
          case "year" => new SFCDimensionDefinition(new TimeDefinition(BinUnit.YEAR), 20)
        }
        Array[SFCDimensionDefinition](dim1, dim2, dim3)
      case _ =>
        Array[SFCDimensionDefinition](
          new SFCDimensionDefinition(new LongitudeDefinition, 31),
          new SFCDimensionDefinition(new LatitudeDefinition(true), 31)
        )
    }
    TieredSFCIndexFactory.createSingleTierStrategy(DIMENSIONS, SFCType.HILBERT)
  }

  /********
   * MAIN *
   ********/
  def main(args: Array[String]): Unit = {

    // Check command line arguments
    if (args.length < 3) {
      println(s"arguments: <n> <period> <mode> ...")
      System.exit(-1)
    }

    // Parse command line arguments
    val n = args(0).toInt
    val period = args(1)
    val cql = args(2) match {
      case "cql" => true
      case "loop" => false
      case s: String => throw new Exception(s"Bad mode: $s")
    }

    // Create strategy
    val strategy = createStrategy(period)

    /*******************
     * PERFORM QUERIES *
     *******************/
    if (cql) { // CQL queries
      val query = CQLQuery.createOptimalQuery(args(3), adapter, null, null)
      val numericData = query.getIndexConstraints(strategy)

      val times = (0 to n).map({ _ =>
        val before = System.currentTimeMillis
        val ranges = strategy.getQueryRanges(numericData.get(0), MAX_RANGE_DECOMPOSITION, null).size
        val after = System.currentTimeMillis

        after - before
      }).drop(1)

      println(s"${times.sum / n.toDouble}")
    }
    else { // "regular" queries
      (6 to 31).foreach({ bits =>
        val fn = { n: String =>
          n match {
            case "bits" => math.pow(0.5, bits)
            case n: String => math.pow(0.5, n.toInt)
          }
        }

        val times = (0 to n).map({ _ =>
          val x1 = (rng.nextDouble * 355) - 180.0
          val y1 = (rng.nextDouble * 175) - 90.0
          val x2 = x1 + 360*fn(args(3))
          val y2 = y1 + 360*fn(args(4))

          val envelope = new Envelope(x1, x2, y1, y2)
          val geom = geometryFactory.toGeometry(envelope)

          val query = period match {
            case "day" | "week" | "year" =>
              val t1 = rng.nextLong % (1000*60*60*24*365)
              val t2 = t1 + fn(args(5)) * (period match {
                case "day" => 1000*60*60*24
                case "week" => 1000*60*60*24*7
                case "year" => 1000*60*60*24*365
              })
              val start = new java.util.Date(t1)
              val end = new java.util.Date(t2.toLong)
              new SpatialTemporalQuery(start, end, geom)
            case _ =>
              new SpatialQuery(geom)
          }

          val numericData = query.getIndexConstraints(strategy)

          val before = System.currentTimeMillis
          val ranges = strategy.getQueryRanges(numericData.get(0), MAX_RANGE_DECOMPOSITION, null).size
          val after = System.currentTimeMillis

          after - before
        }).drop(1)

        println(s"$bits, ${times.sum / n.toDouble}")
      })
    }
  }
}
