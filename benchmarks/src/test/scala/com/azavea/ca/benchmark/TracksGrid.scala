package com.azavea.ca.benchmark

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.collection.JavaConverters._
import scala.util.Random

class TracksGrid extends Simulation {
  val threads   = Integer.getInteger("threads",  500)
  val rampup    = Integer.getInteger("rampup",   10).toLong
  val duration  = Integer.getInteger("duration", 120 * 4).toLong

  val env    = System.getenv().asScala
  val host = "http://tf-lb-20160915173803629434384jdh-2146116008.us-east-1.elb.amazonaws.com"
  val httpConf = http.baseURL(host)

  def gridFeeder(level: Int) = {
    val layoutCols = math.pow(2, level).toInt
    val layoutRows = math.pow(2, level - 1).toInt
    val rnd = new Random(0)

    Iterator.continually(
      Map("z" -> level.toString,
          "timeIndex" -> rnd.nextInt(365/5).toString,
          "col" -> rnd.nextInt(layoutCols).toString,
          "row" -> rnd.nextInt(layoutRows).toString))
  }

  val wm = "m"
  val level = 6

  val target = if (wm == "m") "GeoMesa" else "GeoWave"

  val gridScenario = scenario("Tracks Grid Benchmark")
    .during(duration seconds) {
    feed(gridFeeder(level))
      .exec(http(s"$target Tracks Grid Level: $level, Time Window: 5-day")
              .get("/tracks/grid-query/SET1/5-day/${timeIndex}/${z}/${col}/${row}")
              .queryParam("test", "false")
              .queryParam("waveOrMesa", wm))
  }

  setUp(gridScenario.inject(rampUsers(1) over(10 minute)).protocols(httpConf))
}
