package com.azavea.ingest.common

import org.opengis.feature.simple._
import org.opengis.referencing.operation._
import org.geotools.feature.simple._
import org.geotools.geometry.jts.JTS
import org.geotools.referencing.operation._
import com.vividsolutions.jts.geom.Geometry
import scala.collection.JavaConverters._

object Transform {
  def apply(sft: SimpleFeatureType, sf: SimpleFeature, transform: MathTransform): SimpleFeature = {
    val sfb = new SimpleFeatureBuilder(sft)
    sf
      .getAttributes
      .asScala
      .foreach {
        case geom: Geometry => sfb.add(JTS.transform(geom, transform))
        case obj => sfb.add(obj)
      }

    sfb.buildFeature(null)
  }

  def apply(sf: SimpleFeature, transform: MathTransform): SimpleFeature =
    apply(sf.getType, sf, transform)

}
