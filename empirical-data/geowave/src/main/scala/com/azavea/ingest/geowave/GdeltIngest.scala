package com.azavea.ingest.geowave

import org.opengis.feature.simple._
import org.geotools.feature.simple._
import org.geotools.data.{DataStoreFinder, DataUtilities, FeatureWriter, Transaction}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd._

import geotrellis.spark.util.SparkUtils

import com.azavea.ingest.common._
import com.azavea.ingest.common.csv.HydrateRDD._
import com.azavea.ingest.common.shp.HydrateRDD._


object GdeltIngest {

  val spec ="the_geom=point($55,$54),event_id=int($1),day=date({yyyyMMdd},$2),month_year=int($3),year=int($4),fraction_date=double($5),actor1_code=$6,actor1_name=$7,actor1_country_code=$8,actor1_known_group_code=$9,actor1_ethnic_code=$10,actor1_religion1_code=$11,actor1_religion2_code=$12,actor1_type1_code=$13,actor1_type2_code=$14,actor1_type3_code=$15,actor2_code=$16,actor2_name=$17,actor2_country_code=$18,actor2_known_group_code=$19,actor2_ethnic_code=$20,actor2_religion1_code=$21,actor2_religion2_code=$22,actor2_type1_code=$23,actor2_type2_code=$24,actor2_type3_code=$25,is_root_event=$26,event_code=$27,event_base_code=$28,event_root_code=$29,quad_class=$30,goldstein_scale=double($31),num_mentions=int($32),num_sources=int($33),num_articles=int($34),avg_tone=double($35),actor1_geo_type=int($36),actor1_geo_fullname=$37,actor1_geo_countrycode=$38,actor1_adm1code=$39,actor1_geom=point($41,$40),actor1_geo_featureid=$42,actor2_geo_type=int($43),actor2_geo_fullname=$44,actor2_geo_countrycode=$45,actor2_adm1code=$46,actor2_geom=point($48,$47),actor2_geo_featureid=$49,action_geo_type=int($50),action_geo_fullname=$51,action_geo_countrycode=$52,action_adm1code=$53,action_geo_featureid=$56"


  def main(args: Array[String]): Unit = {
    val zookeeper = args(0)

    val conf: SparkConf =
      new SparkConf()
        .setAppName("GeoWave ingest utility (gdelt)")
        .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
        .set("spark.kryo.registrator", "geotrellis.spark.io.kryo.KryoRegistrator")

    implicit val sc = new SparkContext(conf)

    val params = Ingest.Params(
      Ingest.CSV,
      "gis",
      zookeeper,
      "root",
      "secret",
      "geowave.gdelt",
      0,
      "\t",
      CSVSchemaParser.SpecParser(spec),
      featureName = "gdelt-event",
      s3bucket = "geotrellis-sample-datasets",
      s3prefix = "gdelt/",
      csvExtension = ".gz",
      temporal = true,
      pointOnly = true,
      calcStats = false//,
      // period = "year",
      // numPartitions = 4,
      // partitionStrategy = "HASH"
    )
    println("Params initialized")
    val tybuilder = new SimpleFeatureTypeBuilder
    tybuilder.setName(params.featureName)
    params.codec.genSFT(tybuilder)
    val sft = tybuilder.buildFeatureType
    println("Feature type built for ingest")

    val urls = getCsvUrls(params.s3bucket, params.s3prefix, params.csvExtension, true)
    val linesRdd = csvUrlsToLinesRdd(urls, params.dropLines, 5000)
    val sfRdd: RDD[SimpleFeature] = csvLinesToSfRdd(params.codec, linesRdd, params.separator, params.featureName)
    println("SimpleFeature RDD constructed")

    Ingest.ingestRDD(params)(sfRdd)
  }

}
