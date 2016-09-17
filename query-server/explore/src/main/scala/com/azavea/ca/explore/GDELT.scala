package com.azavea.ca.explore

import com.azavea.ca.server._

object GDELT extends CAQueryUtils {
  val gwTableName = "geowave.gdelt"
  val gwFeatureTypeName = "gdelt-event"

  val gmTableName = "geomesa.gdelt"
  val gmFeatureTypeName = "gdelt-event"
}
