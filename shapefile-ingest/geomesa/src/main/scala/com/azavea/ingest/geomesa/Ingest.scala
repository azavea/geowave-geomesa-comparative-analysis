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
  case class Params (csvOrShp: String = "",
                     instanceId: String = "geomesa",
                     zookeepers: String = "zookeeper",
                     user: String = "root",
                     password: String = "GisPwd",
                     tableName: String = "",
                     dropLines: Int = 0,
                     separator: String = "\t",
                     codec: CSVSchemaParser.Expr = CSVSchemaParser.Spec(Nil),
                     tyBuilder: SimpleFeatureTypeBuilder = new SimpleFeatureTypeBuilder,
                     s3bucket: String = "",
                     s3prefix: String = "",
                     csvExtension: String = ".csv") {

    def convertToJMap(): HashMap[String, String] = {
      val result = new HashMap[String, String]
      result.put("instanceId", instanceId)
      result.put("zookeepers", zookeepers)
      result.put("user", user)
      result.put("password", password)
      result.put("tableName", tableName)
      result
    }
  }

  val parser = new scopt.OptionParser[Params]("geowave-csv-ingest") {
    // for debugging; prevents exit from sbt console
    override def terminate(exitState: Either[String, Unit]): Unit = ()

    head("geowave-csv-ingest", "0.1")

    opt[String]('i',"instance")
      .action( (s, conf) => conf.copy(instanceId = s) )
      .text("Accumulo instance ID [default=geomesa]")
    opt[String]('z',"zookeepers")
      .action( (s, conf) => conf.copy(zookeepers = s) )
      .text("Comma-separated list of zookeepers [default=zookeeper]")
    opt[String]('u',"user")
      .action( (s, conf) => conf.copy(user = s) )
      .text("User namer [default=root]")
    opt[String]('p',"password")
      .action( (s, conf) => conf.copy(password = s) )
      .text("Password [default=GisPwd]")
    opt[String]('t',"table")
      .action( (s, conf) => conf.copy(tableName = s) )
      .required
      .text("Accumulo namespace (should start with 'geomesa.')")
    help("help").text("Display this help message")

    note("")

    cmd("csv")
      .action( (_, conf) => conf.copy(csvOrShp = "csv") )
      .children(
        opt[Int]('d',"drop")
          .action( (i, conf) => conf.copy(dropLines = i) )
          .text("Number of header lines to drop [default=0]"),
        opt[String]('s',"separator")
          .action( (s, conf) => conf.copy(separator = s) )
          .text("Field separator for input text file [default=<Tab>]"),
        opt[String]('e',"extension")
          .action( (s, conf) => conf.copy(csvExtension = s) )
          .text("Delimited file extension [default: '.csv']"),
        opt[String]("featurename")
          .action( (s, conf) => { conf.tyBuilder.setName(s) ; conf })
          .required
          .text("Name for the SimpleFeatureType"),
        opt[String]("codec")
          .action( (s, conf) => conf.copy(codec = CSVSchemaParser.SpecParser(s)) )
          .required
          .text("Codec description for SimpleFeature (see below)"),
        note("")
      )

    cmd("shapefile")
      .action( (_, conf) => conf.copy(csvOrShp = "shp") )

    arg[String]("<s3 bucket>")
      .action( (s, conf) => conf.copy(s3bucket = s) )
      .text("Target Amazon S3 bucket")

    arg[String]("<s3 dir>")
      .action( (s, conf) => conf.copy(s3prefix = s) )
      .text("Directory in S3 bucket containing target files")

    note("""
Codecs are defined as comma-separated list of `key=value' pairs.  A value may
take one of several forms:
    (1) `$n', where n gives the column number of the desired field (n >= 1),
        resulting in a string value.
    (2) `f(args)', where f is a function name and args are a comma-separated
        list of values, and f is chosen from the following set:
                int     string -> integer
                double  string -> double
                point   (double, double) -> point
                concat  (string, ..., string) -> string
                date    (string, string) -> date
    (3) `{string}', which describes a string literal, delimited by braces.
For example, given a file with the day and time in columns 6 and 7,
respectively, this can be converted to a timestamp in the codec using the
expression
              timestamp=date({yyyy-MM-ddHH:mm:ss}, concat($6, $7))

Note: `date' takes a format string compatible with java.text.SimpleDateFormat.
""")
  }



  def registerSFTs(cli: Params)(rdd: RDD[SimpleFeature]) =
    rdd.foreachPartition({ featureIter =>
      val ds = DataStoreFinder.getDataStore(cli.convertToJMap)

      if (ds == null) {
        println("Could not build AccumuloDataStore")
        java.lang.System.exit(-1)
      }

      featureIter.toStream.map({feature =>
        feature.getFeatureType
      }).distinct.foreach({ sft =>
        ds.createSchema(sft)
        ds.dispose
      })
    })

  def ingestRDD(cli: Params)(rdd: RDD[SimpleFeature]) =
    rdd.foreachPartition({ featureIter =>
      val ds = DataStoreFinder.getDataStore(cli.convertToJMap)

      if (ds == null) {
        println("Could not build AccumuloDataStore")
        java.lang.System.exit(-1)
      }

      // The method for ingest here is based on:
      // https://github.com/locationtech/geomesa/blob/master/geomesa-tools/src/main/scala/org/locationtech/geomesa/tools/accumulo/ingest/AbstractIngest.scala#L104
      featureIter.toStream.groupBy(_.getName.toString).foreach({ case (typeName: String, features: SimpleFeature) =>
        val fw = ds.getFeatureWriterAppend(typeName, Transaction.AUTO_COMMIT)
        features.foreach({ feature =>
          val toWrite = fw.next()
          toWrite.setAttributes(feature.getAttributes)
          toWrite.getIdentifier.asInstanceOf[FeatureIdImpl].setID(feature.getID)
          toWrite.getUserData.putAll(feature.getUserData)
          toWrite.getUserData.put(Hints.USE_PROVIDED_FID, java.lang.Boolean.TRUE)
          try {
            fw.write()
          } catch {
            case e: Exception =>
              println(s"Failed to write a feature", e)
          } finally {
            fw.close()
          }
        })
      })

      ds.dispose
    })

  def registerAndIngestRDD(cli: Params)(rdd: RDD[SimpleFeature]) = {
    registerSFTs(cli)(rdd)
    ingestRDD(cli)(rdd)
  }
}
