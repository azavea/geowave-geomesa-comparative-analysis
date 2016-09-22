package com.azavea.ca.core

import geotrellis.proj4._
import geotrellis.vector._
import geotrellis.vector.io._
import geotrellis.vector.io.json._

import spray.json._

case class CityName(name: String)

object CityName {
  implicit object CityNameJsonReader extends JsonReader[CityName] {
    def read(value: JsValue): CityName =
      value.asJsObject.getFields("city") match {
        case Seq(JsString(name)) =>
          CityName(name)
        case v =>
          throw new DeserializationException("CityName expected, got $v")
      }
  }
}

object Cities {
  def buffer(p: Point, d: Double): Polygon =
    p.reproject(LatLng, WebMercator).buffer(d).reproject(WebMercator, LatLng)

  def cityBuffers(name: String): Map[String, Polygon] = {
    val p = citiesByName(name)
    Seq(1, 5, 15, 25, 35, 45, 55, 65)
      .map { z =>
        val m = z * 10000
      (s"${z*10}-KM", buffer(p, m))
      }
      .toMap
  }

  val cities: Vector[PointFeature[CityName]] =
    Resource("cities.geojson")
      .parseGeoJson[JsonFeatureCollection]
      .getAllPointFeatures[CityName]

  val citiesByName: Map[String, Point] =
    cities
      .map { case Feature(geom, data) => (data.name, geom) }
      .toMap
}
