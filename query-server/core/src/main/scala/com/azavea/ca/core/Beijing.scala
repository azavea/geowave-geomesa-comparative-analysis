package com.azavea.ca.core

import geotrellis.vector._
import geotrellis.vector.io._

object Beijing {
  val geom = Resource("beijing.geojson").parseGeoJson[MultiPolygon]

  val boundingBox = geom.envelope
  val boundingBoxGeom = boundingBox.toPolygon

  def boundingBoxes(dimension: Int): Seq[(Int, Int, Extent)] = boundingBoxes(dimension, dimension)
  def boundingBoxes(layoutCols: Int, layoutRows: Int): Seq[(Int, Int, Extent)] = {
    val Extent(xmin, ymin, xmax, ymax) = boundingBox
    val cw = boundingBox.width / layoutCols
    val ch = boundingBox.height / layoutCols
    (for(col <- 0 until layoutCols;
        row <- 0 until layoutRows) yield {
      (col, row,
        Extent(
          xmin + (col * cw),
          ymax - ((row + 1) * ch),
          xmin + ((col + 1) * cw),
          ymax - (row * ch)
        )
      )
    }).toSeq
  }


  object CQL {
    val inBoundingBox = CQLUtils.toBBOXquery("the_geom", boundingBox)
    val notInBoundingBox = s"DISJOINT(the_geom, ${boundingBox.toPolygon.toWKT})"

    val inBeijing = s"INTERSECTS(the_geom, ${geom.toWKT})"
    val notInBeijing = s"DISJOINT(the_geom, ${geom.toWKT})"
  }
}
