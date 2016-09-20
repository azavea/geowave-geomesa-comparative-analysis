package com.azavea.ca.core

import geotrellis.vector._
import geotrellis.vector.io._
import geotrellis.vector.io.json._
import spray.json.DefaultJsonProtocol._
import java.time._

object Tracks {
  val geomUSA = Resource("continental-usa.geojson").parseGeoJson[Polygon]
  val boundingBoxUSA = geomUSA.envelope

  val geomCA = Resource("california.geojson").parseGeoJson[Polygon]
  val boundingBoxCA = geomCA.envelope

  val boundingBox = Extent(-153.11044311523438, -8.030740737915039, 10.138124465942383, 65.8145980834961)
  val boundingBoxGeom = boundingBox.toPolygon

  val gridUSA: Map[Int, Map[(Int, Int), Extent]] = {
    for (level <- 1 to 8) yield {
      val layoutCols = math.pow(2, level).toInt
      val layoutRows = math.pow(2, level - 1).toInt
      val boxes = boundingBoxes(boundingBoxUSA, layoutCols, layoutRows).map { case (col, row, ext) => (col, row) -> ext }
      level -> boxes.toMap
    }
  }.toMap

  def boundingBoxes(extent: Extent, layoutCols: Int, layoutRows: Int): Seq[(Int, Int, Extent)] = {
    val Extent(xmin, ymin, xmax, ymax) = extent
    val cw = extent.width / layoutCols
    val ch = extent.height / layoutRows
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

  def printGrid(level: Int) = {
    JsonFeatureCollection(
      gridUSA(level).map { case ((col, row), ext) =>
        PolygonFeature(ext.toPolygon, s"$col-$row")
      }
    ).toJson.prettyPrint
  }

  val firstDate = LocalDateTime.of(2015,1,1,0,0,1).atZone(ZoneOffset.UTC)

  object CQL {
    def forExtent(extent: Extent) = s"INTERSECTS(the_geom, ${extent.toPolygon.toWKT})"
    def forTime(start: ZonedDateTime, end: ZonedDateTime) = s"TimeStamp DURING $start/${end.minusSeconds(2)}"
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
