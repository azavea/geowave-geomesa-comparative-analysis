package com.azavea.ca.synthetic


class CommonPoke {

  val either = "either"
  val extent = "extent"
  val point = "point"

  var seed: Long = 0

  def decode(inst: String) = {
    /* schema,tasks,lng,lat,time,width */
    inst.split(",").map(_.toLowerCase) match {
      case Array(schema: String, tasks: String, lng: String, lat: String, time: String, width: String) =>
        (0 until tasks.toInt).map({ _ =>
          (schema, {seed += 1; seed}, lng, lat, time, width) })
      /* Error */
      case str =>
        val e = s"Expected <schema,tasks,lng,lat,time,width>; found <$str>"
        throw new Exception(e)
    }
  }

}
