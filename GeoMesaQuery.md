# GeoMesa Indexing and Querying #

## The GeoMesa Index ##

![img2](https://cloud.githubusercontent.com/assets/11281373/17036137/f76a4028-4f58-11e6-98f1-45e995c1ca15.png)

![accumulo-key](https://cloud.githubusercontent.com/assets/11281373/17036145/fe4acd4a-4f58-11e6-9932-03aaa376410e.png)

The `Row ID` seems to be of variable length: [the first 2 bytes are the epoch if there is no split, and the first 3 bytes are the concatenation of the split number (not shown in the picture above) and the epoch if there is a split](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/index/Z3IdxStrategy.scala#L142-L147).

The [split number](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/tables/Z3Table.scala#L193) is the modulus of the hash of the object and the number of splits.

The epoch is a 16-bit number representing the week ([presumably since the Unix epoch](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/tables/Z3Table.scala#L36)).

For points, X, Y, and Time are fixed at [21, 21, and 20 bits](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-z3/src/main/scala/org/locationtech/geomesa/curve/Z3SFC.scala#L17-L19) of precision, respectively.
Time is considered at the resolution of 1 second, and 20 bits is just enough to count the number of seconds in one week.

In the case of "complex geometries", the resolution seems to be [24 bits total](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/tables/Z3Table.scala#L45-L52), rather than the 62 bits total for points.


## Insert ##

[`featureStore.addFeatures(featureCollection);`](https://github.com/geomesa/geomesa-tutorials/blob/293cd73c64b55a23f301065e2e50f696ae6a80bc/geomesa-quickstart-accumulo/src/main/java/com/example/geomesa/accumulo/AccumuloQuickStart.java#L212)

1. [AccumuloFeatureStore.addFeatures](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/AccumuloFeatureStore.scala#L30-L52)
2. [AccumuloDataStore.getFeatureWriterAppend](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/AccumuloDataStore.scala#L398-L417)
3. [AppendAccumuloFeatureWriter constructor](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/AccumuloFeatureWriter.scala#L143-L167)
4. ...
4. AppendAccumuloFeatureWriter.writer which is actually [AccumuloFeatureWriter.writer](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/AccumuloFeatureWriter.scala#L108-L114)
5. [AccumuloFeatureWriter.getTablesAndWriters](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/AccumuloFeatureWriter.scala#L50-L54)
6. [Z3Table.writer](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/tables/Z3Table.scala#L64-L108)
7. [Z3Table.getGeomRowKeys](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/tables/Z3Table.scala#L191-L203)
8. [Z3Table.zBox](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/tables/Z3Table.scala#L205-L219)
9. [Z3Table.zBox](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/tables/Z3Table.scala#L221-L226)
10. [Z3Table.getZPrefixes](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/tables/Z3Table.scala#L230-L250)

Points are inserted with 62 bits of precision.
For objects with extent, their ranges are repeatedly subdivided until ranges with prefixes at least 24 bits in length are found, and the object is associated with each of those prefixes.

## GeoMesa Query Planning ##

### Accumulo ###

In this section we analyze the [following query](https://github.com/geomesa/geomesa-tutorials/blob/293cd73c64b55a23f301065e2e50f696ae6a80bc/geomesa-quickstart-accumulo/src/main/java/com/example/geomesa/accumulo/AccumuloQuickStart.java#L254)

```java
FeatureIterator featureItr = featureSource.getFeatures(query).features();
```

assuming that the `SimpleFeature`s that are being looked for are geometries with extent.
The result of that line is an iterator of `SimpleFeature`s which are responsive to the query.

Restricting attention to query planning, the line above produces the sequence below

1. [AccumuloFeatureSource.getFeatures](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/AccumuloFeatureSource.scala#L87)
2. [AccumuloFeatureSource.getFeatureSource.getFeaturesNoCache](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/AccumuloFeatureSource.scala#L106-L107)
3. [AccumuloFeatureCollection constructor](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/AccumuloFeatureSource.scala#L117-L139)
4. ...
5. [AccumuloFeatureCollection.reader](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/AccumuloFeatureSource.scala#L172-L173)
6. [AccumuloDataStore.getFeatureReader](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/AccumuloDataStore.scala#L350-L360)
7. [AccumuloFeatureReader.apply](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/AccumuloFeatureReader.scala#L47-L58)
8. [AccumuloFeatureReaderImpl constructor](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/data/AccumuloFeatureReader.scala#L47-L58)
9. ...
10. [QueryPlanner.runQuery](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/index/QueryPlanner.scala#L74-L82) 
11. [QueryPlanner.getQueryPlans](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/index/QueryPlanner.scala#L114-L165)
12. [QueryStrategyDecider.chooseStrategies](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/index/QueryStrategyDecider.scala#L34-L107)
13. [QueryStrategyDecider.createStrategy](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/index/QueryStrategyDecider.scala#L141-L155)
14. [Z3IdxStrategy.getQueryPlan](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/index/Z3IdxStrategy.scala#L31-L184)
15. [Z3IdxStrategy.getQueryPlan.getRanges](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/index/Z3IdxStrategy.scala#L137-L138)
   - Called once per prefix, where [prefixes are a function of the epoch (week) and the split](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/index/Z3IdxStrategy.scala#L142-L147)
16. [Z3IdxStrategy.getGeomRanges](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-accumulo/geomesa-accumulo-datastore/src/main/scala/org/locationtech/geomesa/accumulo/index/Z3IdxStrategy.scala#L198-L209)
17. [Z3SFC.ranges](https://github.com/locationtech/geomesa/blob/bab330add6e21ed2c528101d38236a1ca4088c49/geomesa-z3/src/main/scala/org/locationtech/geomesa/curve/Z3SFC.scala#L29-L38)
18. org.locationtech.sfcurve.Z3.zranges which is actually [org.locationtech.sfcurve.ZN.zranges](https://github.com/locationtech/sfcurve/blob/46c668ec9c037a017f5f487d8c00064fc60ee52d/zorder/src/main/scala/org/locationtech/sfcurve/zorder/ZN.scala#L112-L140)

### Cassandra ###

No example in the [GeoMesa tutorial](https://github.com/geomesa/geomesa-tutorials/tree/293cd73c64b55a23f301065e2e50f696ae6a80bc), it is unclear whether the Cassandra code is complete.

### HBase ###

TODO.  Has strong similarities to Accumulo, but is definitely different.
