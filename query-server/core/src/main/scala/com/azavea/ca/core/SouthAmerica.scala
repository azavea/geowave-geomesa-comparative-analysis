package com.azavea.ca.core

import geotrellis.vector._
import geotrellis.vector.io._
import geotrellis.vector.io.json._

import spray.json._

case class SouthAmericaCountry(name: String)

object SouthAmericaCountry {
  implicit object SouthAmericaCountryJsonReader extends JsonReader[SouthAmericaCountry] {
    def read(value: JsValue): SouthAmericaCountry =
      value.asJsObject.getFields("admin") match {
        case Seq(JsString(name)) =>
          SouthAmericaCountry(name.replace(" ", "-"))
        case v =>
          throw new DeserializationException("SouthAmericaCountry expected, got $v")
      }
  }
}

object SouthAmerica {
  val countries: Vector[MultiPolygonFeature[SouthAmericaCountry]] = {
    val collection = Resource("south-america.geojson").parseGeoJson[JsonFeatureCollection]
    (collection.getAllMultiPolygonFeatures[SouthAmericaCountry] ++ collection.getAllPolygonFeatures[SouthAmericaCountry].map(_.mapGeom(MultiPolygon(_))))
  }

  val countriesByName: Map[String, MultiPolygon] =
    countries
      .map { case Feature(geom, data) => (data.name, geom) }
      .toMap

  val geom = countries.map(_.geom).unionGeometries.as[MultiPolygon].get

  val boundingBox = geom.envelope
  val boundingBoxGeom = boundingBox.toPolygon

  object CQL {
    val inBoundingBox = CQLUtils.toBBOXquery("the_geom", boundingBox)
    val notInBoundingBox = s"DISJOINT(the_geom, ${boundingBoxGeom.toWKT})"

    val inSouthAmerica = s"INTERSECTS(the_geom, ${geom.toWKT})"
    val notInSouthAmerica = s"DISJOINT(the_geom, ${geom.toWKT})"

  }
}
