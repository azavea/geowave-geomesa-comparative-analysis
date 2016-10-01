package com.azavea.ca.benchmark

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.feeder.RecordSeqFeederBuilder
import scala.concurrent.duration._
import scala.collection.JavaConverters._
import scala.util.Random


object CitiesSimulation {
  val IS_TEST = "false"
  val TEST_CONTEXT = "MT3"

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

  def citiesParams =
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

  def countriesParams =
    for {
      size <- sizes
      country <- southAmericanCountries
      year <- years
    } yield Map(
      "country" -> country,
      "size" -> size,
      "year" -> year
    )


  def citiesFeeder = {
    val rnd = new scala.util.Random
    Iterator.continually{
      citiesParams(rnd.nextInt(citiesParams.length))
    }
  }

  def countriesFeeder = {
    val rnd = new scala.util.Random
    Iterator.continually {
      countriesParams(rnd.nextInt(countriesParams.length))
    }
  }
}

class GdeltStress(target: GeoTarget, host: String) extends Simulation {
  import CitiesSimulation._

  val httpConf = http.baseURL(host)
  val duration  = 20.minutes
  val users = 8

  setUp(
    scenario(s"City Buffers: ${TEST_CONTEXT} ${target.name}").during(duration) {
      feed(citiesFeeder).exec {
        http("${name}")
          .get(s"/cities/spatiotemporal/${TEST_CONTEXT}/" + "${name}")
          .queryParam("city", "${city}")
          .queryParam("size", "${size}")
          .queryParam("year", "${year}")
          .queryParam("test", IS_TEST)
          .queryParam("wOrm", target.tag)
        }
      }.inject(atOnceUsers(users)),
    scenario(s"Countries ${TEST_CONTEXT} ${target.name}").during(duration) {
       feed(countriesFeeder).exec {
         http("${country}")
           .get(s"/cities/spatiotemporal/${TEST_CONTEXT}/in-south-america-countries-three-weeks")
           .queryParam("country", "${country}")
           .queryParam("size", "${size}")
           .queryParam("year", "${year}")
           .queryParam("test", IS_TEST)
           .queryParam("wOrm", target.tag)
         }
       }
       .inject(atOnceUsers(users))
  ).protocols(httpConf)
}

class WaveGdeltStress extends GdeltStress(
  target = GeoWave,
  host = "http://tf-lb-20160930144456278182977zdu-1322523926.us-east-1.elb.amazonaws.com"
)

class MesaV2GdeltStress extends GdeltStress (
  target = GeoMesa,
  host = "http://tf-lb-20160930193253991640372343-1540097209.us-east-1.elb.amazonaws.com"
)

class MesaV3GdeltStress extends GdeltStress (
  target = GeoMesa,
  host = "http://tf-lb-20160930220530447354535arq-954826942.us-east-1.elb.amazonaws.com"
)

// GDELT_V2    host = "http://tf-lb-20160930193253991640372343-1540097209.us-east-1.elb.amazonaws.com"
  // GDELT_V3 host = "http://tf-lb-20160930220530447354535arq-954826942.us-east-1.elb.amazonaws.com"
