package com.azavea.ingest.common

import org.apache.spark._
import org.geotools.referencing.operation.transform.AffineTransform2D
import org.apache.spark.rdd.RDD
import org.opengis.feature.simple._
import org.geotools.feature.simple._
import org.geotools.referencing.operation.matrix._
import org.geotools.feature._
import geotrellis.vector._


object TranslateRDD {
  def affineTransform(from: Point, to: Point): AffineTransform2D = {
    val shift =  new java.awt.geom.AffineTransform
    shift.translate(to.x - from.x, to.y - from.y)
    new AffineTransform2D(shift)
  }

  /** Shift features such that their origin shifts to a set of points */
  def apply(rdd: RDD[SimpleFeature], origin: Point, shifts: Seq[Point])(implicit sc: SparkContext) =
    rdd.mapPartitions { features =>
      val transforms = for (p <- shifts) yield affineTransform(origin, p)

      for {
        feature <- features
        transform <- transforms
      } yield Transform(feature, transform)
    }
}
