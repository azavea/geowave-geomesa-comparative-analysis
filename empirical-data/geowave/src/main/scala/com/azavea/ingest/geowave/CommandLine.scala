package com.azavea.ingest.geowave

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

import java.util.HashMap
import scala.collection.JavaConversions._
import scala.util.Try

import com.azavea.ingest.common._

object CommandLine {

  val parser = new scopt.OptionParser[Ingest.Params]("geowave-csv-ingest") {
    // for debugging; prevents exit from sbt console
    override def terminate(exitState: Either[String, Unit]): Unit = ()

    head("geowave-csv-ingest", "0.1")

    cmd("csv")
      .action( (_, conf) => conf.copy(csvOrShp = Ingest.CSV) )
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
        opt[String]("codec")
          .action( (s, conf) => conf.copy(codec = CSVSchemaParser.SpecParser(s)) )
          .required
          .text("Codec description for SimpleFeature (see below)"),
        note("")
      )

    cmd("shapefile")
      .action( (_, conf) => conf.copy(csvOrShp = Ingest.SHP) )

    cmd("shp")
      .action( (_, conf) => conf.copy(csvOrShp = Ingest.SHP) )

    note("Global options:\n")

    opt[String]('i',"instance")
      .action( (s, conf) => conf.copy(instanceId = s) )
      .text("Accumulo instance ID [default=geowave]")
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
      .text("Accumulo namespace (should start with 'geowave.')")
    opt[String]('f', "featurename")
      .action( (s, conf) => { conf.copy(featureName = s) })
      .required
      .text("Name for the SimpleFeatureType")
    opt[Unit]('z', "temporal")
      .action( (_, conf) => { conf.copy(temporal = true) })
      .text("Index this as a spatio-temporal dataset.")
    opt[Unit]('x', "point")
      .action( (_, conf) => { conf.copy(pointOnly = true) })
      .text("Index this as a point-only dataset.")

    opt[Int]('r', "numPartitions")
      .action( (v, conf) => { conf.copy(numPartitions = v) })
      .text("Number of partitions for partitioning strategy")
    opt[String]('y', "partitionStrategy")
      .action( (v, conf) => { conf.copy(partitionStrategy = v) })
      .text("Partition strategy, must have numPartitions > 1, one of: HASH, ROUND_ROBIN")
    opt[Int]('w', "numSplits")
      .action( (v, conf) => { conf.copy(numSplits = Some(v)) })
      .text("Number of splits to set up")

    help("help").text("Display this help message")
    note("")

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
}
