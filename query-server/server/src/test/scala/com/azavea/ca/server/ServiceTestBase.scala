package com.azavea.ca.server

import akka.event.{ LoggingAdapter, NoLogging }
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{ Matchers, WordSpec }
import org.scalamock.scalatest.MockFactory

trait ServiceTestBase extends WordSpec with Matchers with ScalatestRouteTest with MockFactory {
  protected def log: LoggingAdapter = NoLogging
}
