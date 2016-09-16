package com.azavea.ingest.common

import com.vividsolutions.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature
import org.geotools.feature.simple._
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem
import org.opengis.referencing.operation.MathTransform;
import scala.collection.JavaConverters._

object Reproject {
  def apply(sf: SimpleFeature, crs: CoordinateReferenceSystem): SimpleFeature = {
    val srcCrs = sf.getType.getCoordinateReferenceSystem
    if (srcCrs == null) return sf // can't reproject without known CRS
    val sftb = new SimpleFeatureTypeBuilder()
    sftb.init(sf.getType)
    sftb.setCRS(crs)
    val sft = sftb.buildFeatureType
    val transform = CRS.findMathTransform(srcCrs, crs, true)
    Transform(sft, sf, transform)
  }
}
