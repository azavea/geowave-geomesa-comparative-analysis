package com.azavea.ca.server.results

import mil.nga.giat.geowave.core.store.CloseableIterator
import mil.nga.giat.geowave.core.store.query.aggregate.CountResult
import org.opengis.feature.simple.SimpleFeature
import org.geotools.data.simple.SimpleFeatureCollection

case class TestResult(
  clusterId: String,
  startTime: Long,
  endTime: Long,
  timeAtFirstResult: Long, // -1 if not used
  duration: Int,
  result: String
)

object TestResult {
  def apply(
    clusterId: String,
    startTime: Long,
    endTime: Long,
    result: String
  ): TestResult = TestResult(clusterId, startTime, endTime, -1L, (endTime - startTime).toInt, result)

  def apply(
    clusterId: String,
    startTime: Long,
    endTime: Long,
    timeAtFirstResult: Option[Long],
    result: String
  ): TestResult = TestResult(clusterId, startTime, endTime, timeAtFirstResult.getOrElse(-1L), (endTime - startTime).toInt, result)

  def capture[T](clusterId: String)(f: (() => Unit) => T)(implicit ev: T => { def toString: String }): TestResult = {
    val before = System.currentTimeMillis
    var timeAtFirstResult: Option[Long] = None
    val captureTimeAtFirstResult = { () => timeAtFirstResult = Some(System.currentTimeMillis) }
    val r = f(captureTimeAtFirstResult)
    val after = System.currentTimeMillis
    TestResult(clusterId, before, after, timeAtFirstResult, r.toString)
  }

  def capture(clusterId: String, iterator: => CloseableIterator[CountResult]): TestResult =
    capture(clusterId) { captureFirst =>
      val itr = iterator
      var first = true
      var n: Long = 0
      while (itr.hasNext) {
        val countResult = itr.next
        n += countResult.getCount
        if(first) {
          captureFirst()
          first = false
        }
      }

      itr.close
      n
    }

  def capture(clusterId: String, iterator: => CloseableIterator[SimpleFeature])(implicit d: DummyImplicit): TestResult =
    capture(clusterId) { captureFirst =>
      val itr = iterator
      var n: Long = 0
      while (itr.hasNext) {
        val feature = itr.next
        n += 1
        if(n == 1L) { captureFirst() }
      }

      itr.close
      n
    }

  def capture(clusterId: String, collection: => SimpleFeatureCollection)(implicit d1: DummyImplicit, d2: DummyImplicit): TestResult =
    capture(clusterId) { captureFirst =>
      val itr = collection.features()
      var n: Long = 0
      while (itr.hasNext) {
        val feature = itr.next
        n += 1
        if(n == 1L) { captureFirst() }
      }

      itr.close
      n
    }
}
