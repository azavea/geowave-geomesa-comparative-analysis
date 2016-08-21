package com.azavea.common


class CommonPoke {

  val either = "either"
  val extent = "extent"
  val point = "point"

  val eitherSft = CommonSimpleFeatureType("Geometry")
  val extentSft = CommonSimpleFeatureType("Polygon")
  val pointSft = CommonSimpleFeatureType("Point")

  var seed: Long = 0

  def decode(inst: String) = {
    /* schema,tasks,lng,lat,time,width */
    inst.split(",").map(_.toLowerCase) match {
      case Array(schemaStr: String, tasks: String, lng: String, lat: String, time: String, width: String) =>
        val schema = schemaStr match {
          case `either` => either
          case `extent` => extent
          case `point` => point
          case str =>
            val e = s"Expected <$either>, <$extent>, <$point>; found <$str>"
            throw new Exception(e)
        }
        (0 until tasks.toInt).map({ _ =>
          (schema, {seed += 1; seed}, lng, lat, time, width) })
      /* Error */
      case str =>
        val e = s"Expected <schema,tasks,lng,lat,time,width>; found <$str>"
        throw new Exception(e)
    }
  }

}
