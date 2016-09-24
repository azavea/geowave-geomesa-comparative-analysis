package com.azavea.ca.benchmark

trait GeoTarget {
  val tag: String
  val name: String
}

case object GeoMesa extends GeoTarget {
  val name = "GeoMesa"
  val tag = "m"
}

case object GeoWave extends GeoTarget {
  val name = "GeoWave"
  val tag = "w"
}
