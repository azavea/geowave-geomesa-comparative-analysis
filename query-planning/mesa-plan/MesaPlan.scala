package com.azavea.ca.planning

import com.vividsolutions.jts.geom._
import org.apache.accumulo.core.client.mock.MockInstance
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.apache.accumulo.core.data.Range
import org.apache.accumulo.core.security.Authorizations
import org.geotools.data.{ DataStoreFinder, Query }
import org.geotools.feature.simple.SimpleFeatureTypeBuilder
import org.geotools.filter.text.cql2.CQL
import org.geotools.referencing.CRS
import org.locationtech.geomesa.accumulo.data.AccumuloDataStore
import org.locationtech.geomesa.accumulo.index._
import org.locationtech.geomesa.accumulo.index.Strategy.StrategyType
import org.locationtech.geomesa.accumulo.index.Strategy.StrategyType.StrategyType
import org.locationtech.geomesa.utils.geotools.RichSimpleFeatureType.RichSimpleFeatureType
import org.locationtech.geomesa.utils.geotools.SimpleFeatureTypes
import org.opengis.feature.simple.SimpleFeatureType

import scala.collection.JavaConverters._


/***************************************************************************************************************************
 * docker run -it --rm -p 50095:50095 --net=geowave --hostname leader --name leader jamesmcclain/geomesa:1.2.6             *
 * docker run -it --rm --net=geowave -v $SPARK_HOME:/spark:ro -v $(pwd)/mesa-plan/target/scala-2.11:/jars:ro openjdk:8-jdk *
 ***************************************************************************************************************************/
object MesaPlan {

  val dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

  // https://gitter.im/locationtech/geomesa?at=57ed4843c512d9653a53515d
  def rangeLength(range: Range): Double = {
    // https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/tables/Z3Table.scala#L177
    val start: Array[Byte] = range.getStartKey.getRowData.toArray.drop(3).take(8)
    val end: Array[Byte] = range.getEndKey.getRowData.toArray.drop(3).take(8)
    val startBI = new java.math.BigInteger(start ++ Array.fill[Byte](8 - start.length)(0))
    val endBI = new java.math.BigInteger(end ++ Array.fill[Byte](8 - end.length)(0))

    endBI.subtract(startBI).doubleValue
  }

  /********
   * MAIN *
   ********/
  def main(args: Array[String]): Unit = {

    // Check command line arguments
    if (args.length < 8) {
      println(s"arguments: <instance> <zookeeper> <user> <password> <table> <n> <period> <mode> ...")
      System.exit(-1)
    }

    // Parse command line arguments
    val n = args(0+5).toInt
    val period = args(1+5)
    val cql = args(2+5) match {
      case "cql" => true
      case "loop" => false
      case s: String => throw new Exception(s"Bad mode: $s")
    }

    // SimpleFeatureType
    val authorityFactory = CRS.getAuthorityFactory(true)
    val epsg4326 = authorityFactory.createCoordinateReferenceSystem("EPSG:4326")
    val sftb = (new SimpleFeatureTypeBuilder).minOccurs(1).maxOccurs(1).nillable(false)
    val sftName = "CommonSimpleFeatureType"
    sftb.setName(sftName)
    sftb.add("when", classOf[java.util.Date])
    sftb.setCRS(epsg4326)
    sftb.add("where", classOf[Point])
    val sft = sftb.buildFeatureType
    sft.getUserData.put(Constants.SF_PROPERTY_START_TIME, "when")
    period match {
      case "day" | "week" | "month" | "year" =>
        sft.getUserData().put("geomesa.z3.interval", period)
      case _ =>
    }

    // Create DataStore
    val dsConf = new java.util.HashMap[String, String]()
    dsConf.put("instanceId", args(0))
    dsConf.put("zookeepers", args(1))
    dsConf.put("user", args(2))
    dsConf.put("password", args(3))
    dsConf.put("tableName", args(4))
    val ds = DataStoreFinder.getDataStore(dsConf).asInstanceOf[AccumuloDataStore]
    ds.createSchema(sft)

    val rng = new scala.util.Random

    /**************************
     * PERFORM QUERY PLANNING *
     **************************/
    if (cql) {
      val filterString = args(3+5)
      val filter = CQL.toFilter(filterString)
      val filterQuery = if (filterString.contains("when")) {
        QueryFilter(StrategyType.Z3, Some(filter))
      } else {
        QueryFilter(StrategyType.Z2, Some(filter))
      }
      val strategy = if (filterString.contains("when")) {
        new Z3IdxStrategy(filterQuery)
      } else {
        new Z2IdxStrategy(filterQuery)
      }

      val data = (0 to n).map({ _ =>
        val qp = QueryPlanner(sft, ds)
        val _query = new Query(sftName, filter)
        QueryPlanner.configureQuery(_query, sft)
        val query = QueryPlanner.updateFilter(_query, sft)

        val hints = query.getHints
        val output = ExplainNull

        val before = System.currentTimeMillis
        val ranges = strategy.getQueryPlan(qp, hints, output).ranges
        val after = System.currentTimeMillis

        (after - before, ranges.size, ranges.map(rangeLength).sum)
      }).drop(1)
      val times = data.map(_._1)
      val rangeCounts = data.map(_._2)
      val rangeLengths = data.map(_._3)

      println(s"${times.sum / n.toDouble}, ${rangeCounts.head}, ${rangeLengths.head}")
    }
    else {
      (2 to 31).foreach({ bits =>
        val fn = { n: String =>
          n match {
            case "bits" => math.pow(0.5, bits)
            case n: String => math.pow(0.5, n.toInt)
          }
        }

        val data = (0 to n).map({ _ =>
          val x1 = (rng.nextDouble * 270) - 180.0
          val y1 = (rng.nextDouble * 90) - 90.0
          val x2 = x1 + 360*fn(args(3+5))
          val y2 = y1 + 360*fn(args(4+5))

          val filterText = s"BBOX(where, $x1, $y1, $x2, $y2)" + (period match {
            case "day" | "week" | "month" | "year" =>
              val t1 = rng.nextLong % (1000*60*60*24*365)
              val t2 = t1 + fn(args(5+5)) * (period match {
                case "day" => 1000*60*60*24
                case "week" => 1000*60*60*24*7
                case "month" => 1000*60*60*24*30
                case "year" => 1000*60*60*24*365
              })
              val start = new java.util.Date(t1)
              val end = new java.util.Date(t2.toLong)
              s" AND (when DURING ${dateFormat.format(start)}/${dateFormat.format(end)})"
            case _ => ""
          })

          val filter = CQL.toFilter(filterText)

          val filterQuery = period match {
            case "day" | "week" | "month" | "year" =>
              QueryFilter(StrategyType.Z3, Some(filter))
            case _ =>
              QueryFilter(StrategyType.Z2, Some(filter))
          }

          val strategy = period match {
            case "day" | "week" | "month" | "year" =>
              new Z3IdxStrategy(filterQuery)
            case _ =>
              new Z2IdxStrategy(filterQuery)
          }

          val qp = QueryPlanner(sft, ds)
          val _query = new Query(sftName, filter)
          QueryPlanner.configureQuery(_query, sft)
          val query = QueryPlanner.updateFilter(_query, sft)

          val hints = query.getHints
          val output = ExplainNull

          val before = System.currentTimeMillis
          val ranges = strategy.getQueryPlan(qp, hints, output).ranges
          val after = System.currentTimeMillis

          (after - before, ranges.size, ranges.map(rangeLength).sum)
        }).drop(1)
        val times = data.map(_._1)
        val rangeCounts = data.map(_._2)
        val rangeLengths = data.map(_._3)

        println(s"$bits, ${times.sum / n.toDouble}, ${rangeCounts.sum / n.toDouble}, ${rangeLengths.sum / n}")
      })
    }
  }
}
