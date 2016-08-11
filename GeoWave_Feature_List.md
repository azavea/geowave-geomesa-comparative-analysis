# GeoWave Feature List #

## Input ##

- Ingest from the CLI
   - Ingest from filesystem -> GeoWave
      - Pointer: [LocalToGeowaveCommand.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/core/ingest/src/main/java/mil/nga/giat/geowave/core/ingest/operations/LocalToGeowaveCommand.java)
      - Notes: Tested, believed to work.
   - Ingest from filesystem -> HDFS -> GeoWave
      - Pointer: [LocalToMapReduceToGeowaveCommand.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/core/ingest/src/main/java/mil/nga/giat/geowave/core/ingest/operations/LocalToMapReduceToGeowaveCommand.java)
      - Notes: Not tested, status unknown.
   - Stage from filesystem -> HDFS
      - Pointer: [LocalToHdfsCommand.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/core/ingest/src/main/java/mil/nga/giat/geowave/core/ingest/operations/LocalToHdfsCommand.java)
      - Notes: Not tested, status unknown.
   - Stage from filesystem -> Kafka
      - Pointer: [LocalToKafkaCommand.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/core/ingest/src/main/java/mil/nga/giat/geowave/core/ingest/operations/LocalToKafkaCommand.java)
      - Notes: Not tested, status unknown.
   - Ingest from Kafka -> GeoWave
      - Pointer: [KafkaToGeowaveCommand.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/core/ingest/src/main/java/mil/nga/giat/geowave/core/ingest/operations/KafkaToGeowaveCommand.java)
      - Notes: Not tested, status unknown.
   - Ingest from HDFS -> GeoWave
      - Pointer: [MapReduceToGeowaveCommand.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/core/ingest/src/main/java/mil/nga/giat/geowave/core/ingest/operations/MapReduceToGeowaveCommand.java)
      - Notes: Not tested, status unknown.
   - Pointer: [GeoWaveMain.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/core/cli/src/main/java/mil/nga/giat/geowave/core/cli/GeoWaveMain.java)
   - Notes: Requires plugins for each input file format, jars must be in the classpath.  The source code for those formats can be found in the `extensions/formats` directory.
- Ingest Using the API
   - Bulk
      - Pointer: [AccumuloKeyValuePairGenerator.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/extensions/datastores/accumulo/src/main/java/mil/nga/giat/geowave/datastore/accumulo/util/AccumuloKeyValuePairGenerator.java)
      - Notes: For data already stored on HDFS, given an appropriate InputFormat, it is possible to create a class derived from org.apache.hadoop.mapreduce.Mapper that can be used to bulk-insert the data into Accumulo (with appropriate GeoWave keys and values).
   - Piecemeal
      - Pointer: [IndexWriter.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/core/store/src/main/java/mil/nga/giat/geowave/core/store/IndexWriter.java)
      - Notes: Given an appropriate DataStore, Adapter, and Index, it is possible to produce an IndexWriter that can be used to write items one-by-one into the DataStore, using the Adapater, and according to the Index.
- File Formats Supported
   - avro
   - gdelt
   - geolife
   - geotools-raster (GeoTools-supported raster data)
   - geotools-vector (GeoTools-supported vector data)
   - gpx
   - stanag4676
   - tdrive
   - Via Extensions:
      - [Landsat 8](https://github.com/ngageoint/geowave/tree/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/extensions/cli/landsat8)
      - [OpenStreetMap](https://github.com/ngageoint/geowave/tree/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/extensions/cli/osm)

## Backends ##

- HBase
- Accumulo

## Integrations ##

- [mrgeo](https://github.com/ngageoint/mrgeo/tree/master/mrgeo-dataprovider/mrgeo-dataprovider-geowave) (reading)
- [GeoTrellis (prospective)](https://github.com/geotrellis/geotrellis/pull/1542) (reading and writing)
- Via [C++ bindings](https://ngageoint.github.io/geowave/documentation.html#jace-jni-proxies-2)
   - [PDAL](https://github.com/PDAL/PDAL/tree/master/plugins/geowave) ([reading and writing](https://ngageoint.github.io/geowave/documentation.html#pdal))
   - mapnik ([reading](https://ngageoint.github.io/geowave/documentation.html#mapnik))

## Secondary Indices ##

- Numerical
   - Pointer: [NumericSecondaryIndexConfiguration.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/extensions/adapters/vector/src/main/java/mil/nga/giat/geowave/adapter/vector/index/NumericSecondaryIndexConfiguration.java)
- Temporal
   - Pointer: [TemporalSecondaryIndexConfiguration.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/extensions/adapters/vector/src/main/java/mil/nga/giat/geowave/adapter/vector/index/TemporalSecondaryIndexConfiguration.java)
- Textual
   - Pointer: [TextSecondaryIndexConfiguration.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/extensions/adapters/vector/src/main/java/mil/nga/giat/geowave/adapter/vector/index/TextSecondaryIndexConfiguration.java)
- User Defined
   - Pointer: [SimpleFeatureUserDataConfiguration.java](https://github.com/ngageoint/geowave/blob/a367dbed823417f8bf2ace7e6a180522d4018d55/extensions/adapters/vector/src/main/java/mil/nga/giat/geowave/adapter/vector/utils/SimpleFeatureUserDataConfiguration.java)
   - Examples:
      - [TimeDescriptionConfiguration](https://github.com/ngageoint/geowave/blob/master/extensions/adapters/vector/src/main/java/mil/nga/giat/geowave/adapter/vector/utils/TimeDescriptors.java#L147-L283) allows indexing over intervals
      - [VisibilityConfiguration.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/extensions/adapters/vector/src/main/java/mil/nga/giat/geowave/adapter/vector/plugin/visibility/VisibilityConfiguration.java)
      - [StatsConfigurationCollection.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/extensions/adapters/vector/src/main/java/mil/nga/giat/geowave/adapter/vector/stats/StatsConfigurationCollection.java)
- [No Cost-Based Optimization](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/core/store/src/main/java/mil/nga/giat/geowave/core/store/index/SecondaryIndexQueryManager.java#L8-L9)

## Processing ##

- k-means
   - CLI
      - Pointer: [KmeansParallelCommand.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/analytics/mapreduce/src/main/java/mil/nga/giat/geowave/analytic/mapreduce/operations/KmeansParallelCommand.java)
   - Map-Reduce
      - Pointer: [KMeansMapReduce.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/analytics/mapreduce/src/main/java/mil/nga/giat/geowave/analytic/mapreduce/kmeans/KMeansMapReduce.java)
      - Notes: Means are supported, it appears that medians and centers are not.
- Jump Method (k-discovery)
   - CLI
      - Pointer: [KmeansJumpCommand.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/analytics/mapreduce/src/main/java/mil/nga/giat/geowave/analytic/mapreduce/operations/KmeansJumpCommand.java)
   - Map-Reduce
      - Pointer: [KMeansDistortionMapReduce.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/analytics/mapreduce/src/main/java/mil/nga/giat/geowave/analytic/mapreduce/kmeans/KMeansDistortionMapReduce.java)
      - Notes: Uses the approach given in [Catherine A. Sugar; Gareth M. James (2003). "Finding the number of clusters in a data set: An information theoretic approach". Journal of the American Statistical Association 98 (January): 750–763](http://amstat.tandfonline.com/doi/abs/10.1198/016214503000000666) with the common covariance matrix set to the identity matrix.
- Sampling
   - Map-Reduce
      - Pointer: [KSamplerMapReduce.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/analytics/mapreduce/src/main/java/mil/nga/giat/geowave/analytic/mapreduce/kmeans/KSamplerMapReduce.java)
      - Notes: Chooses k random features from either the overall collection of features or from a (some) group(s) of features.
- Kernel Density Estimation
   - CLI
      - Pointer: [KdeCommand.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/analytics/mapreduce/src/main/java/mil/nga/giat/geowave/analytic/mapreduce/operations/KdeCommand.java)
   - Map-Reduce
      - Pointer: [AccumuloKDEReducer.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/analytics/mapreduce/src/main/java/mil/nga/giat/geowave/analytic/mapreduce/kde/AccumuloKDEReducer.java)
      - Notes: General background can be found [on the Wikipedia page](https://en.wikipedia.org/wiki/Kernel_density_estimation).  This implementation uses Gaussian basis functions.
- Nearest Neighbors
   - CLI
      - Pointer: [NearestNeighborCommand.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/analytics/mapreduce/src/main/java/mil/nga/giat/geowave/analytic/mapreduce/operations/NearestNeighborCommand.java)
   - Map-Reduce
      - Pointer: [NNMapReduce.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/analytics/mapreduce/src/main/java/mil/nga/giat/geowave/analytic/mapreduce/nn/NNMapReduce.java)
      - Notes: Appears to use partitioned direct search.
- Clustering
   - Map-Reduce
      - Pointer: [GroupAssignmentMapReduce.java]https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/analytics/mapreduce/src/main/java/mil/nga/giat/geowave/analytic/mapreduce/kmeans/KSamplerMapReduce.java)
- Convex Hulls of Clusters
   - Map-Reduce
      - Pointer: [ConvexHullMapReduce.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/analytics/mapreduce/src/main/java/mil/nga/giat/geowave/analytic/mapreduce/clustering/ConvexHullMapReduce.java)
- DBSCAN
   - Map-Reduce
      - Pointer: [DBScanMapReduce.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/analytics/mapreduce/src/main/java/mil/nga/giat/geowave/analytic/mapreduce/dbscan/DBScanMapReduce.java)
      - Notes: See [the Wikipedia page](https://en.wikipedia.org/wiki/DBSCAN) for general background.  This implementation deviates from the standard approach.  See [this](https://github.com/ngageoint/geowave/blob/7390c28b9418b2602ef72f2ddc7f285fc600c4f3/analytics/mapreduce/src/main/java/mil/nga/giat/geowave/analytic/mapreduce/dbscan/DBScanMapReduce.java#L47-L82) code comment for details.
- Spark Support
   - Pointer: [analytics/spark/src/main/scala/mil/nga/giat/geowave/analytics/spark](https://github.com/ngageoint/geowave/tree/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/analytics/spark/src/main/scala/mil/nga/giat/geowave/analytics/spark)
   - Pointer: [AnalyticRecipes.scala](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/analytics/spark/src/main/scala/mil/nga/giat/geowave/analytics/spark/tools/AnalyticRecipes.scala)
   - Notes: The analytic recipes file provides tidbits useful for addressing clustering-related questions with GeoWave and Spark.

## Output ##

- GeoServer Plugin
   - Pointer: [GeoserverServiceImpl.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/services/webapp/src/main/java/mil/nga/giat/geowave/service/impl/GeoserverServiceImpl.java)
   - Notes: This can be used to view GeoWave Layers in GeoServer.  Using an [SLD](https://en.wikipedia.org/wiki/Styled_Layer_Descriptor) such as [this one](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/examples/example-slds/DecimatePoints.sld) allows large datasets to be shown interactively by subsampling at the pixel level
- Query
   - RDD
      - Pointer: [GeoWaveInputFormat.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/core/mapreduce/src/main/java/mil/nga/giat/geowave/mapreduce/input/GeoWaveInputFormat.java)
      - Notes: One can use the GeoWaveInputFormat class to perform a query which returns an RDD of key, value pairs.  The procedure for doing that is to construct GeoWave Query and QueryOptions objects then insert them into a org.apache.hadoop.conf.Configuration object using static methods on GeoWaveInputFormat, the passing that configuration object into a call to the newAPIHadoopRDD method.
   - Iterator
      - Pointer: [DataStore.java](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/core/store/src/main/java/mil/nga/giat/geowave/core/store/DataStore.java)
      - Notes: It is possible to perform a query which returns an iterator of values.  Given a DataStore, a Query, and a QueryOptions, one uses the query method on the DataStore class.
