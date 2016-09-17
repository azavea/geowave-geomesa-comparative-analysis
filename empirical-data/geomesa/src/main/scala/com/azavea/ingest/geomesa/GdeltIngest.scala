package com.azavea.ingest.geomesa

import org.opengis.feature.simple._
import org.geotools.feature.simple._
import org.geotools.data.{DataStoreFinder, DataUtilities, FeatureWriter, Transaction}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd._
import org.locationtech.geomesa.compute.spark.GeoMesaSpark

import geotrellis.spark.util.SparkUtils

import com.azavea.ingest.common._
import com.azavea.ingest.common.csv.HydrateRDD._
import com.azavea.ingest.common.shp.HydrateRDD._


object GdeltIngest {

  val spec ="the_geom=point($31,$32),event_id=int($1),day=date({yyyyMMdd},$2),month_year=int($3),year=int($4),fraction_date=double($5),actor1_code=$6,actor1_name=$7,actor1_country_code=$8,actor1_known_group_code=$9,actor1_ethnic_code=$10,actor1_religion1_code=$11,actor1_religion2_code=$12,actor1_type1_code=$13,actor1_type2_code=$14,actor1_type3_code=$15,is_root_event=$16,event_code=$17,event_base_code=$18,event_root_code=$19,quad_class=$20,goldstein_scale=double($21),num_mentions=int($22),num_sources=int($23),num_articles=int($24),avg_tone=double($25),actor1_geo_type=int($26),actor1_geo_fullname=$27,actor1_geo_countrycode=$28,actor1_adm1code=$29,actor1_geo_featureid=$32"

  def main(args: Array[String]): Unit = {
    println("Initializing gdelt ingest")
    val zookeeper = args(0)

    val params = Ingest.Params(
      Ingest.CSV,
      "gis",
      zookeeper,
      "root",
      "secret",
      "geomesa.gdelt",
      0,
      "\t",
      CSVSchemaParser.SpecParser(spec),
      featureName = "gdelt-event",
      s3bucket = "geotrellis-sample-datasets",
      s3prefix = "gdelt/",
      csvExtension = ".gz"
    )
    println("Params initialized")
    val tybuilder = new SimpleFeatureTypeBuilder
    tybuilder.setName(params.featureName)
    params.codec.genSFT(tybuilder)
    val sft = tybuilder.buildFeatureType
    println("Feature type built for ingest")

    val conf: SparkConf = (GeoMesaSpark.init(new SparkConf(), Seq(sft)))
      .setAppName("GeoMesa ingest utility")

    implicit val sc: SparkContext = new SparkContext(conf)

    val urls = getCsvUrls(params.s3bucket, params.s3prefix, params.csvExtension, true)
    val linesRdd = csvUrlsToLinesRdd(urls, params.dropLines, 5000)
    val sfRdd = csvLinesToSfRdd(params.codec, linesRdd, params.separator, params.featureName)
    println("SimpleFeature RDD constructed")

    Ingest.ingestRDD(params)(sfRdd)
  }

}
