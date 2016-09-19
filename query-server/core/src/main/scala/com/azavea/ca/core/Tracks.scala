package com.azavea.ca.core

import geotrellis.vector._
import geotrellis.vector.io._

object Tracks {
  val geomUSA = Resource("continental-usa.geojson").parseGeoJson[Polygon]
  val boundingBoxUSA = geomUSA.envelope

  val geomCA = Resource("california.geojson").parseGeoJson[Polygon]
  val boundingBoxCA = geomCA.envelope

  val boundingBox = Extent(-153.11044311523438, -8.030740737915039, 10.138124465942383, 65.8145980834961)
  val boundingBoxGeom = boundingBox.toPolygon

  def boundingBoxesUSA(dimension: Int): Seq[(Int, Int, Extent)] = boundingBoxes(boundingBoxUSA, dimension, dimension)
  def boundingBoxes(extent: Extent, dimension: Int): Seq[(Int, Int, Extent)] = boundingBoxes(extent, dimension, dimension)
  def boundingBoxes(extent: Extent, layoutCols: Int, layoutRows: Int): Seq[(Int, Int, Extent)] = {
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


  val tmsLayouts = {

  }

  object CQL {
    val inBoundingBox = CQLUtils.toBBOXquery("the_geom", boundingBox)
    val notInBoundingBox = s"DISJOINT(the_geom, ${boundingBox.toPolygon.toWKT})"

    val inUSA = s"INTERSECTS(the_geom, ${geomUSA.toWKT})"
    val notInUSA = s"DISJOINT(the_geom, ${geomUSA.toWKT})"
    val inBoundingBoxUSA = CQLUtils.toBBOXquery("the_geom", boundingBoxUSA)

    val inCA = s"INTERSECTS(the_geom, ${geomCA.toWKT})"
    val notInCA = s"DISJOINT(the_geom, ${geomCA.toWKT})"
    val inBoundingBoxCA = CQLUtils.toBBOXquery("the_geom", boundingBoxCA)
  }

}
