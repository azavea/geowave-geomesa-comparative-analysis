package com.azavea.ingest.common

import org.apache.spark._
import org.apache.spark.rdd.RDD
import org.geotools.feature.DefaultFeatureCollection
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}

import java.io.{BufferedReader, File, InputStreamReader}
import java.util.zip.GZIPInputStream
import scala.util.Try

object CSVtoSimpleFeature {

  def parseCSVFile(schema: CSVSchemaParser.Expr,
                   url: java.net.URL,
                   drop: Int,
                   delim: String,
                   sftName: String,
                   features: DefaultFeatureCollection,
                   unzip: Boolean) = {

    val reader =
      if (unzip) new BufferedReader(new InputStreamReader(new GZIPInputStream(url.openStream)))
      else new BufferedReader(new InputStreamReader(url.openStream))

    val iter = reader.lines.iterator

    // drop first lines
    for (i <- 0 until drop) { iter.next }

    val name = (url.getFile.split("/").reverse)(0)
    var i = features.size
    while (iter.hasNext) {
      val row: Array[String] = iter.next.split(delim)

      val feature = schema.makeSimpleFeature(sftName, row, java.util.UUID.randomUUID.toString)
      features.add(feature)
      i += 1
    }

    reader.close
  }
}
