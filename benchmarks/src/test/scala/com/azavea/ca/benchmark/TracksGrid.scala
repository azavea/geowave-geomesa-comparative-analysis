package com.azavea.ca.benchmark

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.collection.JavaConverters._
import scala.util.Random


object TracksSimulation {
  val set = "SET1"
  val isTest = "false"

  def gridFeeder(level: Int, timeResolution: Int) = {
    val layoutCols = math.pow(2, level).toInt
    val layoutRows = math.pow(2, level - 1).toInt
    val rnd = new Random(0)

    Iterator.continually(
      Map("z" -> level.toString,
          "timeIndex" -> rnd.nextInt(timeResolution).toString,
          "col" -> rnd.nextInt(layoutCols).toString,
          "row" -> rnd.nextInt(layoutRows).toString))
  }

  def gridScenario(target: GeoTarget, level: Int, timeWindow: String, timeResolution: Int, duration: Duration) = {
    val name = s"${target.name} Tracks Grid Level: $level, Time Window: $timeWindow"
    scenario(name)
      .during(duration) {
      feed(gridFeeder(level, timeResolution))
        .exec(http(name)
                .get("/tracks/grid-query/"+set+"/"+timeWindow+"/${timeIndex}/${z}/${col}/${row}")
                .queryParam("test", isTest)
                .queryParam("waveOrMesa", target.tag))
    }
  }
}

trait TracksSimulation extends Simulation {
  val host = "http://tf-lb-20160915173803629434384jdh-2146116008.us-east-1.elb.amazonaws.com"
  val httpConf = http.baseURL(host)
}

class GeoMesa_Level4_1Month extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoMesa, 4, "1-month", 12, 120 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoWave_Level4_1Month extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoWave, 4, "1-month", 12, 120 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoMesa_Level5_1Month extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoMesa, 5, "1-month", 12, 240 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoWave_Level5_1Month extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoWave, 5, "1-month", 12, 240 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoMesa_Level6_1Month extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoMesa, 6, "1-month", 12, 240 * 2 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoWave_Level6_1Month extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoWave, 6, "1-month", 12, 240 * 2 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoMesa_Level7_1Month extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoMesa, 7, "1-month", 12, 240 * 2 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoWave_Level7_1Month extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoWave, 7, "1-month", 12, 240 * 2 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

//--- 1 week
class GeoMesa_Level4_1Week extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoMesa, 4, "1-week", 52, 120 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoWave_Level4_1Week extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoWave, 4, "1-week", 52, 120 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoMesa_Level5_1Week extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoMesa, 5, "1-week", 52, 240 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoWave_Level5_1Week extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoWave, 5, "1-week", 52, 240 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoMesa_Level6_1Week extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoMesa, 6, "1-week", 52, 240 * 2 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoWave_Level6_1Week extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoWave, 6, "1-week", 52, 240 * 2 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoMesa_Level7_1Week extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoMesa, 7, "1-week", 52, 240 * 3 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoWave_Level7_1Week extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoWave, 7, "1-week", 52, 240 * 3 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

// 18 days
class GeoMesa_Level4_18Days extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoMesa, 4, "18-day", 20, 120 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoWave_Level4_18Days extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoWave, 4, "18-day", 20, 120 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoMesa_Level5_18Days extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoMesa, 5, "18-day", 20, 240 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoWave_Level5_18Days extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoWave, 5, "18-day", 20, 240 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoMesa_Level6_18Days extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoMesa, 6, "18-day", 20, 240 * 2 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoWave_Level6_18Days extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoWave, 6, "18-day", 20, 240 * 2 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoMesa_Level7_18Days extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoMesa, 7, "18-day", 20, 240 * 3 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoWave_Level7_18Days extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoWave, 7, "18-day", 20, 240 * 3 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

// 27 days
class GeoMesa_Level4_27Days extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoMesa, 4, "27-day", 13, 120 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoWave_Level4_27Days extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoWave, 4, "27-day", 13, 120 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoMesa_Level5_27Days extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoMesa, 5, "27-day", 13, 240 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoWave_Level5_27Days extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoWave, 5, "27-day", 13, 240 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoMesa_Level6_27Days extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoMesa, 6, "27-day", 13, 240 * 2 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoWave_Level6_27Days extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoWave, 6, "27-day", 13, 240 * 2 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoMesa_Level7_27Days extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoMesa, 7, "27-day", 13, 240 * 3 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}

class GeoWave_Level7_27Days extends TracksSimulation {
  setUp(
    TracksSimulation.gridScenario(GeoWave, 7, "27-day", 13, 240 * 3 seconds)
      .inject(rampUsers(1) over(10 minute))
  ).protocols(httpConf)
}
