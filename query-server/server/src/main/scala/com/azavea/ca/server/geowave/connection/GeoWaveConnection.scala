package com.azavea.ca.server.geowave.connection

import com.typesafe.config.ConfigFactory
import mil.nga.giat.geowave.datastore.accumulo._
import mil.nga.giat.geowave.datastore.accumulo.metadata._
import mil.nga.giat.geowave.datastore.accumulo.operations.config._
import net.ceedubs.ficus.Ficus
import net.ceedubs.ficus.readers.ArbitraryTypeReader

import scala.collection.JavaConversions._


object GeoWaveConnection {
  import Ficus._
  import ArbitraryTypeReader._

  private val config = ConfigFactory.load()
  protected lazy val geowaveConfig = config.as[GeoWaveConnectionConfig]("geowave")

  private val additionalAccumuloOpts = new AccumuloOptions
  additionalAccumuloOpts.setUseAltIndex(true)

  lazy val clusterId = geowaveConfig.cluster

  def accumuloOpts(gwNamespace: String) = {
    val opts = new AccumuloRequiredOptions
    opts.setZookeeper(geowaveConfig.zookeepers)
    opts.setInstance(geowaveConfig.instance)
    opts.setUser(geowaveConfig.user)
    opts.setPassword(geowaveConfig.password)
    opts.setGeowaveNamespace(gwNamespace)
    opts.setAdditionalOptions(additionalAccumuloOpts)
  }

  def basicOperations(gwNamespace: String) =
    new BasicAccumuloOperations(
      geowaveConfig.zookeepers,
      geowaveConfig.instance,
      geowaveConfig.user,
      geowaveConfig.password,
      gwNamespace
    )

  def dataStore(gwNamespace: String) =
    new AccumuloDataStore(basicOperations(gwNamespace))

  def adapterStore(gwNamespace: String) =
    new AccumuloAdapterStore(basicOperations(gwNamespace))
}
