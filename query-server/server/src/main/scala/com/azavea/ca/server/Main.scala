package com.azavea.ca.server

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives

import com.azavea.ca.server.exception._

object AkkaSystem {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  trait LoggerExecutor extends BaseComponent {
    protected implicit val executor = system.dispatcher
    protected implicit val log: LoggingAdapter = Logging(system, "app")
  }
}

object Main extends App with Config with AkkaSystem.LoggerExecutor {
  import AkkaSystem._
  import Directives._

  implicit def stackTraceHandler = ExceptionHandling.stackTraceHandler

  Http().bindAndHandle(Routes(), httpConfig.interface, httpConfig.port)
}
