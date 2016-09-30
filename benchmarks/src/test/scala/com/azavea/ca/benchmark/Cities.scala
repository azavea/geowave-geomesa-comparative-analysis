package com.azavea.ca.benchmark

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.feeder.RecordSeqFeederBuilder
import scala.concurrent.duration._
import scala.collection.JavaConverters._
import scala.util.Random


object CitiesSimulation {
  val testContext = "SET1"
  val isTest = "true"

  def cityTests = Vector(
    "in-city-buffers-six-days",
    "in-city-buffers-two-weeks",
    "in-city-buffers-two-months",
    "in-city-buffers-ten-months",
    "in-city-buffers-fourteen-months")

  val cities = Vector(
    "Paris", "Philadelphia", "Istanbul", "Baghdad", "Tehran", "Beijing",
    "Tokyo", "Oslo", "Khartoum", "Johannesburg")

  val southAmericanCountries = Vector(
    "Bolivia", "Falkland-Islands", "Guyana", "Suriname", "Venezuela", "Peru",
    "Ecuador", "Paraguay", "Uruguay", "Chile", "Colombia", "Brazil", "Argentina" )

  val sizes = Vector( 10, 50, 150, 250, 350, 450, 550, 650 )

  val years = (2000 to 2016)

  def citiesParams = Random.shuffle(
    for {
      size <- sizes
      city <- cities
      year <- years
      name <- cityTests
    } yield Map[String, String](
      "city" -> city,
      "size" -> size.toString,
      "year" -> year.toString,
      "name" -> name
    )
  )

  def countriesParams = Random.shuffle(
    for {
      size <- sizes
      country <- southAmericanCountries
      year <- years
    } yield Map(
      "country" -> country,
      "size" -> size,
      "year" -> year
    )
  )

  def citiesFeeder = RecordSeqFeederBuilder(citiesParams)
  def countriesFeeder = RecordSeqFeederBuilder(countriesParams)
}

trait CitiesSimulation extends Simulation {
  val host = "http://tf-lb-201609292204292847903892ty-1166708383.us-east-1.elb.amazonaws.com"
  val httpConf = http.baseURL(host)
  val target = GeoMesa
}

class GdeltStress extends CitiesSimulation {
  import CitiesSimulation._
  val duration  = 1.minutes

  setUp(
    scenario("City Buffers")
      .feed(citiesFeeder.random)
      .during(duration) {
        exec {
          http("${name}")
            .get(s"/cities/gdelt/gdelt-feature/spatiotemporal/${testContext}/" + "${name}")
            .queryParam("city", "${city}")
            .queryParam("size", "${size}")
            .queryParam("year", "${year}")
            .queryParam("test", isTest)
            .queryParam("wOrm", target.tag)
        }
      }
      .inject(atOnceUsers(1)),
    scenario("Countries")
      .feed(countriesFeeder.random)
      .during(duration) {
        exec {
          http("${country}")
            .get(s"/cities/gdelt/gdelt-feature/spatiotemporal/${testContext}/in-south-america-countries-three-weeks")
            .queryParam("country", "${country}")
            .queryParam("size", "${size}")
            .queryParam("year", "${year}")
            .queryParam("test", isTest)
            .queryParam("wOrm", target.tag)
        }
      }
      .inject(atOnceUsers(1))
  ).protocols(httpConf)
}
