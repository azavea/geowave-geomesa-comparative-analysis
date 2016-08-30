package com.azavea.common

import com.vividsolutions.jts.geom._
import org.geotools.factory.Hints
import org.geotools.feature.DefaultFeatureCollection
import org.geotools.feature.simple.SimpleFeatureBuilder
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}

import java.util.GregorianCalendar


object GeometryGenerator {

  import CommonSimpleFeatureType._

  val precisionModel = new PrecisionModel(PrecisionModel.FLOATING)
  val factory = new GeometryFactory(precisionModel, 4326)
  val rng = new scala.util.Random

  type rngType = () => Double
  type rngDateType = () => java.util.Date

  /**
    * Uniform random numbers
    */
  def uniform(rng: scala.util.Random, a: Double, b: Double): rngType = { () =>
    a + (rng.nextDouble * (b - a))
  }

  /**
    * Normally distributed random numbers
    */
  def normal(rng: scala.util.Random, mu: Double, sigma: Double): rngType = { () =>
    mu + (rng.nextGaussian * sigma)
  }

  /**
    * Fixed stream of numbers
    */
  def fixed(x: Double) = { () => x }

  /**
    * Transform a description of a spatial variable into a function
    * which generates a stream of numbers fitting that description.
    */
  def spaceVar(description: String) = {
    description.split(":") match {
      /* Uniform */
      case Array("uniform", a: String, b: String) =>
        uniform(rng, a.toDouble, b.toDouble)
      /* Gaussian */
      case Array("normal", mu: String, sigma: String) =>
        normal(rng, mu.toDouble, sigma.toDouble)
      /* Fixed */
      case Array("fixed", x: String) =>
        fixed(x.toDouble)
      /* Error */
      case str =>
        val e = s"Expected <uniform:a:b>, <normal:μ:σ>, or <fixed:x>; found <$str>"
        throw new Exception(e)
    }
  }

  /**
    * Transform a description of a temporal variable into a function
    * which generates a stream of java.util.Date objects fitting that
    * description.
    */
  def timeVar(description: String): rngDateType = {
    val fn = spaceVar(description)
    val retval = { () => new java.util.Date(fn().toLong) }

    retval
  }


  /**
    * Fill-in a SimpleFeature.  Use the provided time and place, and
    * randomly fill the other fields.
    */
  def fillIn(sf: SimpleFeature, time: java.util.Date, place: Geometry): SimpleFeature = {
    sf.getUserData.put(Hints.USE_PROVIDED_FID, java.lang.Boolean.TRUE)
    sf.setAttribute(whoField, (1 to 3).map({ _ => (rng.nextInt(26) + 'A').toChar }).mkString)
    sf.setAttribute(whatField, (1 to 7).map({ _ => (rng.nextInt(26) + 'A').toChar }).mkString)
    sf.setAttribute(whenField, time)
    sf.setAttribute(whereField, place)
    sf.setAttribute(whyField, (1 to 13).map({ _ => (rng.nextInt(26) + 'A').toChar }).mkString)
    sf
  }

  def apply(
    schema: SimpleFeatureType,
    seed: Long,
    lng: String, lat: String, time: String,
    width: String
  ) = {
    rng.setSeed(seed)

    val fc = new DefaultFeatureCollection

    val lngFn = spaceVar(lng)
    val latFn = spaceVar(lat)
    val timeFn = timeVar(time)

    width.split(":").map(_.toDouble) match {
      /* Extents.  Given in the form lo:hi:step:n, where the range of widths
       * (and heights) of the square extents goes from "lo" to "hi" in
       * increments of "step". "n" is the number of extents of each
       * width that is created. */
      case Array(lo: Double, hi: Double, step: Double, n: Double) =>
        (lo until hi by step).foreach({ width =>
          (0 until n.toInt).foreach({ i =>
          val sf = SimpleFeatureBuilder.build(schema, Array.empty[AnyRef], s"i=$i width=$width")

          val lng = lngFn()
          val lat = latFn()
          val time = timeFn()

          val latMin = math.max(lat, 0)
          val lngMin = math.max(lng, 0)
          val latMax = math.min(latMin + width, 90)
          val lngMax = math.min(lngMin + width, 180)
          val xy1 = new Coordinate(lngMin, latMin)
          val xy2 = new Coordinate(lngMax, latMin)
          val xy3 = new Coordinate(lngMax, latMax)
          val xy4 = new Coordinate(lngMin, latMax)
          val ring = factory.createLinearRing(List(xy1, xy2, xy3, xy4, xy1).toArray)
          val place = factory.createPolygon(ring)

          fc.add(fillIn(sf, time, place))
          })
        })
      /* Points.  The number n is the number of points to generate. */
      case Array(n: Double) =>
        (0 until n.toInt).foreach({ i =>
          val sf = SimpleFeatureBuilder.build(schema, Array.empty[AnyRef], s"i=$i")

          val time = timeFn()
          val lng = lngFn()
          val lat = latFn()

          val xy = new Coordinate(lng, lat)
          val place = factory.createPoint(xy)

          fc.add(fillIn(sf, time, place))
        })
      /* Error */
      case arr =>
        val e = s"Expected <lo:hi:step:n> or <n>; found <${arr.toList}>"
        throw new Exception(e)
    }
    fc
  }

}
