package com.azavea.ingest.common

import org.geotools.feature.DefaultFeatureCollection
import org.geotools.feature.simple.{SimpleFeatureBuilder, SimpleFeatureTypeBuilder}
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}

import java.io.{BufferedReader, File, InputStreamReader}
import scala.util.Try

import com.azavea.ingest.common.CSVSchemaParser.Expr

object CSVtoSimpleFeature {

  def parseCSVFile(schema: Expr, 
                   url: java.net.URL, 
                   drop: Int, 
                   delim: String, 
                   builder: SimpleFeatureBuilder, 
                   features: DefaultFeatureCollection) = {

    val brMaybe = Try(new BufferedReader(new InputStreamReader(url.openStream)))
    if (brMaybe.isFailure) {
      throw new java.lang.IOException
    }
    val iter = brMaybe.get.lines.iterator

    for (i <- 0 until drop) { iter.next }

    val name = (url.getName.split("/").reverse)(0)
    var i = feature.size
    while (iter.hasNext) {
      val row = iter.next.split(delim)

      val feature = schema.makeSimpleFeature(builder, row, name + "-" + i.toString)
      features.add(feature)
      i += 1
    }

    brMaybe.get.close
  }

}
