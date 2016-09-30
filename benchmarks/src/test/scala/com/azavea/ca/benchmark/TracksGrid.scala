package com.azavea.ca.benchmark

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.collection.JavaConverters._
import scala.util.Random


object TracksSimulation {
  val set = "LOL"
  val isTest = "true"

  def gridFeeder(level: Int, timeResolution: Int) = {
    val layoutCols = math.pow(2, level).toInt
    val layoutRows = math.pow(2, level - 1).toInt
    val rnd = new Random(0)

    Iterator.continually(
      Map("z"         -> level.toString,
          "timeIndex" -> rnd.nextInt(timeResolution).toString,
          "col"       -> rnd.nextInt(layoutCols).toString,
          "row"       -> rnd.nextInt(layoutRows).toString))
  }

  def timeFeeder = {
    val windows = Vector(
      "1-month" -> 12,
      "27-day"  -> 13,
      "18-day"  -> 20,
      "9-day"   -> 40,
      "1-week"  -> 52,
      "5-day"   -> 73)

    Iterator.continually {
      val (name, count) = windows(Random.nextInt(windows.length))
      Map (
        "timeWindow" -> name,
        "timeIndex"  -> Random.nextInt(count))
    }
  }

  def levelCells(level: Int): Int =
    math.pow(2, level).toInt * math.pow(2, level - 1).toInt

  def levelFeeder(minLevel: Int, maxLevel: Int) = {
    val rnd = new Random(0)
    val level = weightedSelect(minLevel to maxLevel, levelCells, rnd.nextDouble)
    val layoutCols = math.pow(2, level).toInt
    val layoutRows = math.pow(2, level - 1).toInt

    Iterator.continually(
      Map("z"         -> level,
          "col"       -> rnd.nextInt(layoutCols).toString,
          "row"       -> rnd.nextInt(layoutRows).toString))
  }

  def weightedSelect[T](items: Seq[T], itemWeight: T => Double, point: Double): T = {
    require(point >= 0 && point <= 1.0)
    val weights = items.map(itemWeight)
    val weightedPoint = point * weights.sum
    val cum = weights.foldLeft(List(0.0)){ case (list, x) => (list.head + x) :: list }
    val i = cum.indexWhere( _ < weightedPoint)
    if (i <= 0) items.head
    else items(items.length - i)
  }
}

class TacksStress(host: String, target: GeoTarget) extends Simulation {
  import TracksSimulation._

  val duration = 1 minutes
  val users = 1

  setUp(
    scenario(s"Tracks Pyramid: ${target.name}").during(duration) {
      feed(
        levelFeeder(4,8) zip timeFeeder map { case (a, b) => a ++ b }
      ).exec(
        http("level-${z}-${timeWindow}")
          .get("/tracks/grid-query/"+set+"/${timeWindow}/${timeIndex}/${z}/${col}/${row}")
          .queryParam("test", TracksSimulation.isTest)
          .queryParam("waveOrMesa", target.tag))
    }.inject(atOnceUsers(users))
  ).protocols(http.baseURL(host))
}

class MesaTracksStress extends TracksStress("http://tf-lb-20160930181400416739554j6z-617057458.us-east-1.elb.amazonaws.com", GeoMesa)
