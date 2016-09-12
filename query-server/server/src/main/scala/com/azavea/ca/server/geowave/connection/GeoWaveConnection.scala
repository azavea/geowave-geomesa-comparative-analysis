package com.azavea.ca.server.geowave.connection

import com.typesafe.config.ConfigFactory
import mil.nga.giat.geowave.adapter.vector.plugin.{GeoWaveGTDataStore, GeoWavePluginConfig}
import mil.nga.giat.geowave.datastore.accumulo._
import mil.nga.giat.geowave.datastore.accumulo.metadata._
import mil.nga.giat.geowave.datastore.accumulo.operations.config._
import mil.nga.giat.geowave.core.store.operations.remote.options.DataStorePluginOptions
import net.ceedubs.ficus.Ficus
import net.ceedubs.ficus.readers.ArbitraryTypeReader
import org.geotools.data.{DataStore => GeotoolsDataStore, _}

import scala.collection.JavaConverters._


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

  def geotoolsDataStore(gwNamespace: String): GeotoolsDataStore = {
    val dataStoreOptions= new DataStorePluginOptions()
    val opts = new AccumuloRequiredOptions()
    opts.setGeowaveNamespace(gwNamespace);
    opts.setUser(geowaveConfig.user);
    opts.setPassword(geowaveConfig.password);
    opts.setInstance(geowaveConfig.instance);
    opts.setZookeeper(geowaveConfig.zookeepers);
    dataStoreOptions.selectPlugin(new AccumuloDataStoreFactory().getName())
    dataStoreOptions.setFactoryOptions(opts)

    val config =
      dataStoreOptions.getFactoryOptionsAsMap()
        .asScala
        .map { case (key, value) => (key, value: java.io.Serializable) }
        .toMap
        .asJava

    new GeoWaveGTDataStore(
      new GeoWavePluginConfig(
        dataStoreOptions.getFactoryFamily(),
        config)
    )
  }
}
