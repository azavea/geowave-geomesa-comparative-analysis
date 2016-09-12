package com.azavea.ca.server.exception

import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._

import java.io._

object ExceptionHandling {
  def stackTraceHandler = ExceptionHandler {
    case e: Exception =>
      val sw = new StringWriter()
      val pw = new PrintWriter(sw)
      e.printStackTrace(pw)
      complete(sw.toString())
  }
}
