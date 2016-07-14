> The following document uses a hierarchical structure of features - from high level descriptions of general functionality to the subfeatures of which those general functions are composed.


## Data Ingest/Input

- geomesa-tools (command line tools for interacting with geomesa)
  - Creating a geomesa datastore for accumulo
    - Pointer: [CreateCommand.scala](https://github.com/locationtech/geomesa/blob/b7056fae4988ef524913bf3dc33d9ff2a3476b09/geomesa-tools/src/main/scala/org/locationtech/geomesa/tools/accumulo/commands/CreateCommand.scala)
    - Behavior tested; works
  - Ingest vectors, provided a `GeoMesaInputFormat`
    - Pointer: [IngestCommand.scala](https://github.com/locationtech/geomesa/blob/b7056fae4988ef524913bf3dc33d9ff2a3476b09/geomesa-tools/src/main/scala/org/locationtech/geomesa/tools/accumulo/commands/IngestCommand.scala)
    - Behavior tested; works
  - Ingest rasters
    - Pointer: [IngestRasterCommand.scala](https://github.com/locationtech/geomesa/blob/b7056fae4988ef524913bf3dc33d9ff2a3476b09/geomesa-tools/src/main/scala/org/locationtech/geomesa/tools/accumulo/commands/IngestRasterCommand.scala)
    - Supported file formats: "tif", "tiff", "geotiff", "dt0", "dt1", "dt2"


- geomesa-convert (tools for converting various serialization formats to `SimpleFeature`s for ingestion - conversion mechanisms are specified by way of configuration files)
  - delimited text (usually CSV/TSV)
    - Pointer: [DelimitedTextConverter.scala](https://github.com/locationtech/geomesa/blob/b7056fae4988ef524913bf3dc33d9ff2a3476b09/geomesa-convert/geomesa-convert-text/src/main/scala/org/locationtech/geomesa/convert/text/DelimitedTextConverter.scala)
    - Currently supported formats: "CSV" | "DEFAULT", "EXCEL", "MYSQL", "TDF" | "TSV" | "TAB", "RFC4180", "QUOTED", "QUOTE_ESCAPE", "QUOTED_WITH_QUOTE_ESCAPE". $1 through $n for n values per line ($0 refers to the entire line).
  - fixed width
    - Pointer: [FixedWidthConverters.scala](https://github.com/locationtech/geomesa/blob/b7056fae4988ef524913bf3dc33d9ff2a3476b09/geomesa-convert/geomesa-convert-fixedwidth/src/main/scala/org/locationtech/geomesa/convert/fixedwidth/FixedWidthConverters.scala)
  - avro
    - Pointer: [geomesa-convert-avro](https://github.com/locationtech/geomesa/tree/b7056fae4988ef524913bf3dc33d9ff2a3476b09/geomesa-convert/geomesa-convert-avro/src/main/scala/org/locationtech/geomesa/convert/avro)
  - json
    - Pointer: [geomesa-convert-json](https://github.com/locationtech/geomesa/tree/b7056fae4988ef524913bf3dc33d9ff2a3476b09/geomesa-convert/geomesa-convert-json/src/main/scala/org/locationtech/geomesa/convert/json)
  - xml
    - Pointer: [geomesa-convert-xml](https://github.com/locationtech/geomesa/tree/b7056fae4988ef524913bf3dc33d9ff2a3476b09/geomesa-convert/geomesa-convert-xml/src/main/scala/org/locationtech/geomesa/convert/xml)
- geomesa-stream (support for streaming input)
  - A datastore which listens for updates from a [source which meets certain conditions](https://github.com/locationtech/geomesa/blob/7c295d68bad92291e4260273134219dd0f938faf/geomesa-stream/geomesa-stream-api/src/main/scala/org/locationtech/geomesa/stream/SimpleFeatureStreamSource.scala)
    - Pointer: [StreamDataStore.scala](https://github.com/locationtech/geomesa/blob/master/geomesa-stream/geomesa-stream-datastore/src/main/scala/org/locationtech/geomesa/stream/datastore/StreamDataStore.scala)
    - A generic apache-camel based implementation](https://github.com/locationtech/geomesa/blob/b7056fae4988ef524913bf3dc33d9ff2a3476b09/geomesa-stream/geomesa-stream-generic/src/main/scala/org/locationtech/geomesa/stream/generic/GenericSimpleFeatureStreamSourceFactory.scala)
  - Hooks for updating GeoServer on stream update
    - Pointer: In docs but not implemented? [stub pomfile](https://github.com/locationtech/geomesa/tree/b7056fae4988ef524913bf3dc33d9ff2a3476b09/geomesa-gs-plugin/geomesa-stream-gs-plugin)

## Data Processing

- geomesa-compute
  - Generating `RDD`s of `SimpleFeature`s
      - Pointer: [GeoMesaSpark.scala](https://github.com/locationtech/geomesa/blob/b7056fae4988ef524913bf3dc33d9ff2a3476b09/geomesa-compute/src/main/scala/org/locationtech/geomesa/compute/spark/GeoMesaSpark.scala)
      - Capable of querying with CQL to fill an RDD with some subset of your data
    - Carrying out spark SQL queries to process geomesa data
      - Pointer: [GeoMesaSparkSql.scala](https://github.com/locationtech/geomesa/blob/b7056fae4988ef524913bf3dc33d9ff2a3476b09/geomesa-compute/src/main/scala/org/locationtech/geomesa/compute/spark/sql/GeoMesaSparkSql.scala)
      - When constructing a spark context, "yarn-client" is set to be the master, which isn't always a good assumption
      - As of 7/12/16, some stubbed out functions remain in the [`GeoMesaDataContext`](https://github.com/locationtech/geomesa/blob/b7056fae4988ef524913bf3dc33d9ff2a3476b09/geomesa-compute/src/main/scala/org/locationtech/geomesa/compute/spark/sql/GeoMesaDataContext.scala)
- geomesa-jobs
  - Reading data for use in a custom M/R job
      - Pointer: [geomesa-jobs mapreduce](https://github.com/locationtech/geomesa/blob/master/geomesa-jobs/src/main/scala/org/locationtech/geomesa/jobs/mapreduce/)
      - Pointer: [geomesa-jobs mapred](https://github.com/locationtech/geomesa/blob/master/geomesa-jobs/src/main/scala/org/locationtech/geomesa/jobs/mapred/)
      - Apparently capable of reading from any GeoMesa `DataStore` as well as from the filesystem with or without avro files specifying the details of the conversion.
- geomesa-process - (On Accumulo backed GeoMesa instances only - with the possible exception of `Point2Point` and `DensityProcess`, based on file locations and accumulo imports within said files. All processes are registered in https://github.com/locationtech/geomesa/blob/b7056fae4988ef524913bf3dc33d9ff2a3476b09/geomesa-process/src/main/scala/org/locationtech/geomesa/process/ProcessFactory.scala)
  - computing a heatmap from a provided CQL query
    - Pointer: [DensityProcess.scala](https://github.com/locationtech/geomesa/blob/b7056fae4988ef524913bf3dc33d9ff2a3476b09/geomesa-process/src/main/scala/org/locationtech/geomesa/process/DensityProcess.scala)
  - Given CQL and a description of the stats of interest, compute said stats on said CQL results
    - Pointer: [StatsIteratorProcess.scala](https://github.com/locationtech/geomesa/blob/b7056fae4988ef524913bf3dc33d9ff2a3476b09/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/process/stats/StatsIteratorProcess.scala)
  - 'Tube selection' (space/time correlated queries)
    - Pointer: [geomesa 'tube' queries](geomesa/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/process/tube/)
    - This is a pretty sophisticated query mechanism. The basic idea is that, given a collection of points (with associated times), you should be able to return similar collections of points (in terms of where the lines connecting said points exist). Constraints on the query include the size of the spatial and temporal buffers (this is the sense in which we're dealing with 'tubes') and maximum speed attained by the entity whose points make up a given trajectory. Read more here: http://www.geomesa.org/documentation/tutorials/geomesa-tubeselect.html
  - Proximity Search
    - Pointer: [ProximitySearchProcess.scala](https://github.com/locationtech/geomesa/blob/b7056fae4988ef524913bf3dc33d9ff2a3476b09/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/process/proximity/ProximitySearchProcess.scala)
    - Given a set of vectors to search through and a set of vectors to establish proximity, return the members of the former set which lie within the (specified) proximity of members of the latter set
  - Query against an accumulo GeoMesa store
    - Pointer: [QueryProcess.scala](https://github.com/locationtech/geomesa/blob/b7056fae4988ef524913bf3dc33d9ff2a3476b09/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/process/query/QueryProcess.scala)
    - Takes advantage of accumulo optimization to carry out geomesa queries
  - Find the K nearest neighbors to a given point
    - Pointer: [KNearestNeighborSearchProcess.scala](https://github.com/locationtech/geomesa/blob/b7056fae4988ef524913bf3dc33d9ff2a3476b09/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/process/knn/KNearestNeighborSearchProcess.scala)
  - Identify unique values for an attribute in results of a CQL query
    - Pointer: [UniqueProcess.scala](https://github.com/locationtech/geomesa/blob/b7056fae4988ef524913bf3dc33d9ff2a3476b09/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/process/unique/UniqueProcess.scala)
  - Convert points to lines
    - Pointer: [Point2PointProcess.scala](https://github.com/locationtech/geomesa/blob/b7056fae4988ef524913bf3dc33d9ff2a3476b09/geomesa-process/src/main/scala/org/locationtech/geomesa/process/Point2PointProcess.scala)
    - Convert a collection of points into a collection of line segments given a middle term parameter. Optionally break on the day of occurrence. This feature isn't really advertised.


## Output
- geomesa-accumulo
  - A reader for directly querying a datastore in java/scala
    - Pointer: [<DataStore>.getFeatureReader](https://github.com/locationtech/geomesa/blob/99d2bcf47a2363f58e05abf7f3c39ef214551ed2/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/AccumuloDataStore.scala#L354)
    - This is the best bet for high speed accumulo reads, per the GeoMesa gitter.
  - Produce a collection of features for a given datastore
    - Pointer: [<DataStore>.getFeatureSource](https://github.com/locationtech/geomesa/blob/99d2bcf47a2363f58e05abf7f3c39ef214551ed2/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/AccumuloDataStore.scala#L329)
    - Performance characteristics vs the above reader are unclear. This feature is used, however, in the command line export
- geomesa-tools (command line tools for interacting with geomesa)
  - Serialize and export stored features (vectors)
    - Pointer: [ExportCommand.scala](https://github.com/locationtech/geomesa/blob/b7056fae4988ef524913bf3dc33d9ff2a3476b09/geomesa-tools/src/main/scala/org/locationtech/geomesa/tools/accumulo/commands/ExportCommand.scala)
    - Supported export formats: CSV, shapefile, geojson, GML, BIN, Avro

## Other Features

- HBase backend
- geomesa-cassandra
  - Back a geomesa datastore with cassandra
    - [cassandra datastore](https://github.com/locationtech/geomesa/tree/b7056fae4988ef524913bf3dc33d9ff2a3476b09/geomesa-cassandra/geomesa-cassandra-datastore/src/main/scala/org/locationtech/geomesa/cassandra/data)
    - Docs describe this feature as 'alpha' quality currently
- geomesa-kafka
  - Use kafka backed geomesa datastore to pipe simplefeature types from producers, through kafka, to consumers
  - Details can be found [here](https://github.com/locationtech/geomesa/blob/b7056fae4988ef524913bf3dc33d9ff2a3476b09/docs/user/kafka_datastore.rst)
- Metrics reporting
  - Pointer: [geomesa-metrics](https://github.com/locationtech/geomesa/tree/b7056fae4988ef524913bf3dc33d9ff2a3476b09/geomesa-metrics/src/main/scala/org/locationtech/geomesa/metrics)
  - Real time reporting of performance for GeoMesa instances. Supports multiple reporting backends - Ganglia, Graphite, and CSV/TSV
