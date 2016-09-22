package com.azavea.ca.core

import geotrellis.vector._
import geotrellis.vector.io._

object CQLUtils {
  def toBBOXquery(geomAttrib: String, boundingBox: Extent): String =
    s"BBOX($geomAttrib, ${boundingBox.xmin},${boundingBox.ymin},${boundingBox.xmax},${boundingBox.ymax})"

  def intersects(geomAttrib: String, geom: Geometry): String =
    s"INTERSECTS(${geomAttrib}, ${geom.toWKT})"
}
