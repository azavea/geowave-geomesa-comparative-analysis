package com.azavea.ca.core

import geotrellis.vector._
import geotrellis.vector.io._
import geotrellis.vector.io.json._

object Russia {
  val geom = Resource("Russia.geojson").parseGeoJson[Polygon]

  val boundingBox = geom.envelope
  val boundingBoxGeom = boundingBox.toPolygon

  object CQL {
    val inBoundingBox = CQLUtils.toBBOXquery("the_geom", boundingBox)
    val inRussia = s"INTERSECTS(the_geom, ${geom.toWKT})"
  }
}
