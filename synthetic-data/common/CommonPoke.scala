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
    inst.split(":").map(_.toLowerCase) match {
      case Array(point, "true", n: String, m: String) =>
        (0 until m.toInt).map({ _ => (point, n.toInt, -1.0, true, {seed += 1; seed}) })
      case Array(either, "true", n: String, m: String) =>
        (0 until m.toInt).map({ _ => (either, n.toInt, -1.0, true, {seed += 1; seed}) })
      case Array(point, "false", n: String, m: String) =>
        (0 until m.toInt).map({ _ => (point, n.toInt, -1.0, false, {seed += 1; seed}) })
      case Array(either, "false", n: String, m: String) =>
        (0 until m.toInt).map({ _ => (either, n.toInt, -1.0, false, {seed += 1; seed}) })

      case Array(extent, "true", n: String, lo: String, hi: String, step: String) =>
        (lo.toDouble until hi.toDouble by step.toDouble).map({ meters => (extent, n.toInt, meters, true, {seed += 1; seed}) })
      case Array(either, "true", n: String, lo: String, hi: String, step: String) =>
        (lo.toDouble until hi.toDouble by step.toDouble).map({ meters => (either, n.toInt, meters, true, {seed += 1; seed}) })
      case Array(extent, "false", n: String, lo: String, hi: String, step: String) =>
        (lo.toDouble until hi.toDouble by step.toDouble).map({ meters => (extent, n.toInt, meters, false, {seed += 1; seed}) })
      case Array(either, "false", n: String, lo: String, hi: String, step: String) =>
        (lo.toDouble until hi.toDouble by step.toDouble).map({ meters => (either, n.toInt, meters, false, {seed += 1; seed}) })
    }
  }

}
