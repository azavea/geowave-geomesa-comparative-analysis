package com.azavea.common

import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory, Point}
import org.geotools.factory.Hints
import org.geotools.feature.DefaultFeatureCollection
import org.geotools.feature.simple.SimpleFeatureBuilder
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}

import java.util.GregorianCalendar


object PointGenerator {

  import CommonSimpleFeatureType._

  val factory = new GeometryFactory

  def apply(n: Int, sft: SimpleFeatureType, randomTime: Boolean, seed: Long) = {
    val rng = new scala.util.Random(seed)
    val fc = new DefaultFeatureCollection

    var i = 0; while (i < n) {
      val sf = SimpleFeatureBuilder.build(sft, Array.empty[AnyRef], s"Point $seed:$i")
      val time =
        if (!randomTime) new GregorianCalendar(2012, 11, 21)
        else new GregorianCalendar(
          1970+rng.nextInt(50),
          rng.nextInt(12),
          1+rng.nextInt(28),
          rng.nextInt(24),
          rng.nextInt(60),
          rng.nextInt(60)
        )
      val lng = rng.nextDouble*360-180
      val lat = rng.nextDouble*180-90
      val xy = new Coordinate(lat, lng)
      val place = factory.createPoint(xy)

      sf.getUserData.put(Hints.USE_PROVIDED_FID, java.lang.Boolean.TRUE)
      sf.setAttribute(whoField, List((rng.nextInt(26) + 'A').toChar))
      sf.setAttribute(whatField, (1 to 7).map({ _ => (rng.nextInt(26) + 'A').toChar }).mkString)
      sf.setAttribute(whenField, time)
      sf.setAttribute(whereField, place)
      sf.setAttribute(whyField, (1 to 13).map({ _ => (rng.nextInt(26) + 'A').toChar }).mkString)

      fc.add(sf); i += 1
    }

    fc
  }

}
