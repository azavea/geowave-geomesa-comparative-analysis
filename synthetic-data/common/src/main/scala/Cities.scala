package com.azavea.ca.synthetic

import spray.json._
import spray.json.DefaultJsonProtocol._
import geotrellis.vector._
import geotrellis.vector.io._
import geotrellis.vector.io.json._

object Cities {

  val cities: Vector[Point] =
    Resource("cities.geojson")
      .parseGeoJson[JsonFeatureCollection]
      .getAllPointFeatures[Unit]
      .map(_.geom)

  def geometries(instructions: Seq[String]): Seq[(String, Long, String, String, String, String)]=
    instructions.flatMap { ins =>
      cities.map { city =>
        val latLng = s"normal:${city.x}:0.5,normal:${city.y}:0.5"
        ins.split(",") match {
          case Array(schema, tasks, time, width) =>
            s"$schema,$tasks,$latLng,$time,$width"
        }
      }.flatMap(CommonPoke.decode)
    }
}
