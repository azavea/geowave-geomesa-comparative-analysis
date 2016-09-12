package com.azavea.ca.server

import akka.event.LoggingAdapter

import scala.concurrent.ExecutionContext

trait BaseComponent {
  protected implicit def log: LoggingAdapter
  protected implicit def executor: ExecutionContext
}
