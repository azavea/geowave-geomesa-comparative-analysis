package com.azavea.ca.core

import geotrellis.vector._

object CQLUtils {
  def toBBOXquery(geomAttrib: String, boundingBox: Extent): String =
    s"BBOX($geomAttrib, ${boundingBox.xmin},${boundingBox.ymin},${boundingBox.xmax},${boundingBox.ymax})"
}
