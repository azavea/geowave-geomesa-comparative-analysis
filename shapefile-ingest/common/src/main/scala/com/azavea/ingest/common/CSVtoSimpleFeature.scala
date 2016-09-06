package com.azavea.ingest.common

import org.geotools.feature.DefaultFeatureCollection
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}

import java.io.{BufferedReader, File, InputStreamReader}
import scala.util.Try

import com.azavea.ingest.common.CSVSchemaParser

object CSVtoSimpleFeature {

  def parseCSVFile(schema: CSVSchemaParser.Expr,
                   url: java.net.URL,
                   drop: Int,
                   delim: String,
                   sftName: String,
                   features: DefaultFeatureCollection) = {

    val brMaybe = Try(new BufferedReader(new InputStreamReader(url.openStream)))
    if (brMaybe.isFailure) {
      throw new java.io.IOException
    }
    val iter = brMaybe.get.lines.iterator

    for (i <- 0 until drop) { iter.next }

    val name = (url.getFile.split("/").reverse)(0)
    var i = features.size
    while (iter.hasNext) {
      val row: Array[String] = iter.next.split(delim)

      val feature = schema.makeSimpleFeature(sftName, row, (name + "-" + i.toString))
      features.add(feature)
      i += 1
    }

    brMaybe.get.close
  }
}
