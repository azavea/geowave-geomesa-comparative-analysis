package com.azavea.ca.benchmark

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.feeder.RecordSeqFeederBuilder
import scala.concurrent.duration._
import scala.collection.JavaConverters._
import scala.util.Random


object CitiesSimulation {
  val isTest = "true"

  def cityTests = Vector(
    "in-city-buffers-six-days"
    "in-city-buffers-two-weeks",
    "in-city-buffers-two-months",
    "in-city-buffers-ten-months",
    "in-city-buffers-fourteen-months")

  val cities = Array(
    "Paris", "Philadelphia", "Istanbul", "Baghdad", "Tehran", "Beijing",
    "Tokyo", "Oslo", "Khartoum", "Johannesburg" )

  val southAmericanCountries = Array(
    "Bolivia", "Falkland-Islands", "Guyana", "Suriname", "Venezuela", "Peru",
    "Ecuador", "Paraguay", "Uruguay", "Chile", "Colombia", "Brazil", "Argentina" )

  val sizes = Array( 10, 50, 150, 250, 350, 450, 550, 650 )

  val years = (2000 to 2016)

  def citiesParams = Random.shuffle(
    {for {
      size <- sizes
      city <- cities
      year <- years
    } yield Map[String, String](
      "city" -> city,
      "size" -> size.toString,
      "year" -> year.toString
    )}.toVector
  )

  def countriesFeeder = RecordSeqFeederBuilder(
    for {
      size <- sizes
      country <- southAmericanCountries
      year <- years
    } yield Map(
      "country" -> country,
      "size" -> size,
      "year" -> year
    )
  ).random


  def cityScenario(target: GeoTarget) =
    scenario("City Buffers Mixed Time")
      .foreach(cityTests,"name") {
        foreach(citiesParams, "params") {
          exec {
            http("${name}").get("/cities/spatiotemporal/${name}")
              .queryParamMap("${params}")
              .queryParam("test", isTest)
              .queryParam("wOrm", target.tag)
          }
        }
      }
}

trait CitiesSimulation extends Simulation {
  val host = "http://tf-lb-20160927191945717026634sk6-1641270464.us-east-1.elb.amazonaws.com"
  val httpConf = http.baseURL(host)
  val target = GeoMesa
}

/** Execute the city buffer tests sequentially, randomly selecting test and size */
class CityBuffers extends CitiesSimulation {
  val users = 1
  val duration = 10.minutes
  setUp(
    CitiesSimulation.cityScenario(target).inject(rampUsers(users) over (duration))
  ).protocols(httpConf)
}
