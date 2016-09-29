# GeoWave Indexing and Querying #

## Index ##

![img2](https://cloud.githubusercontent.com/assets/11281373/17036137/f76a4028-4f58-11e6-98f1-45e995c1ca15.png)

![accumulo](https://cloud.githubusercontent.com/assets/11281373/17036141/fbff35c6-4f58-11e6-913f-db1a82be2cac.png)

In the simplest case, if the entire Hilbert index is viewed as a tree, each "tier" can be [identified with a level of that tree](https://github.com/ngageoint/geowave/blob/master/core/index/src/main/java/mil/nga/giat/geowave/core/index/sfc/tiered/TieredSFCIndexFactory.java#L130).
This is configurable (one tier, tiers that are not related to one another, &c), so that interpretation is not certainly true.

The "bin" is a function of space and time.
In the most basic case, as in the case of [BasicDimensionDefinition](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/core/index/src/main/java/mil/nga/giat/geowave/core/index/dimension/BasicDimensionDefinition.java#L47-L56),
it is just a clamping of the range of the data to some already given range (encoded within a descendant of NumericDimensionDefinition [e.g. BasicDimensionDefinition]).

The number of tiers and the precision of the dimensions within those tiers is [configurable](https://github.com/ngageoint/geowave/blob/master/core/index/src/main/java/mil/nga/giat/geowave/core/index/sfc/tiered/TieredSFCIndexFactory.java).


## Insert ##

The following chain of events sketches how row IDs are generated in the case of a [TieredSFCIndexStrategy](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/core/index/src/main/java/mil/nga/giat/geowave/core/index/sfc/tiered/TieredSFCIndexStrategy.java).

1. [TieredSFCIndexStrategy.getInsertionIds](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/core/index/src/main/java/mil/nga/giat/geowave/core/index/sfc/tiered/TieredSFCIndexStrategy.java#L187-L193)
2. [TieredSFCIndexStrategy.internalGetInsertionIds](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/core/index/src/main/java/mil/nga/giat/geowave/core/index/sfc/tiered/TieredSFCIndexStrategy.java#L204-L220)
3. [TieredSFCIndexStrategy.getRowIds](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/core/index/src/main/java/mil/nga/giat/geowave/core/index/sfc/tiered/TieredSFCIndexStrategy.java#L363-L385)
4. [TieredSFCIndexStrategy.getRowIdsAtTier](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/core/index/src/main/java/mil/nga/giat/geowave/core/index/sfc/tiered/TieredSFCIndexStrategy.java#L391-L420)
5. [TieredSFCIndexStrategy.decomposeRangesForEntry](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/core/index/src/main/java/mil/nga/giat/geowave/core/index/sfc/tiered/TieredSFCIndexStrategy.java#L422-L469)

In the majority of cases, [it is expected](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/core/index/src/main/java/mil/nga/giat/geowave/core/index/sfc/tiered/TieredSFCIndexStrategy.java#L366-L368) that an entry will intersect only a single key (or a constant number of keys?) in the GeoWave index (in some appropriate tier).


## Query Planning ##

### Primary Index ###

This section assumes a purely geometry query of the `dataStore.query(...)` style on an Accumulo-backed instance of GeoWave.
It appears that the HBase backend works in a substantially similar way.

```scala
val adapter: RasterDataAdapter = ???
val customIndex: CustomIdIndex = ???
val dataStore: AccumuloDataStore = ???
val geom: Geometry = ???
val queryOptions = new QueryOptions(adapter, customIndex)
val query = new IndexOnlySpatialQuery(geom)
val index = (new SpatialDimensionalityTypeProvider.SpatialIndexBuilder).createIndex()
    
dataStore.query(queryOptions, query)
```

restricting attention to query planning, the code above leads to sequence below

1. AccumuloDataStore.query, which is actually [BasedDataStore.query](https://github.com/ngageoint/geowave/blob/302092385d841a83addcb30c120b03148dfe8a5d/core/store/src/main/java/mil/nga/giat/geowave/core/store/BaseDataStore.java#L155-L259)
2. [BasedDataStore.queryConstraints](https://github.com/ngageoint/geowave/blob/302092385d841a83addcb30c120b03148dfe8a5d/core/store/src/main/java/mil/nga/giat/geowave/core/store/BaseDataStore.java#L553-L559) which is actually [AccumuloDataStore.queryConstraints](https://github.com/ngageoint/geowave/blob/302092385d841a83addcb30c120b03148dfe8a5d/extensions/datastores/accumulo/src/main/java/mil/nga/giat/geowave/datastore/accumulo/AccumuloDataStore.java#L324-L362)
   - [AccumuloConstraintsQuery (constructor)](https://github.com/ngageoint/geowave/blob/302092385d841a83addcb30c120b03148dfe8a5d/extensions/datastores/accumulo/src/main/java/mil/nga/giat/geowave/datastore/accumulo/query/AccumuloConstraintsQuery.java#L75-L108) is called from here
      - The object [base](https://github.com/ngageoint/geowave/blob/302092385d841a83addcb30c120b03148dfe8a5d/extensions/datastores/accumulo/src/main/java/mil/nga/giat/geowave/datastore/accumulo/query/AccumuloConstraintsQuery.java#L97-L105), which will be important later (because it holds the actual query constraints), is created in that constructor.
4. AccumuloConstrainsQuery.query which is actually [AccumuloFilteredIndexQuery.query](https://github.com/ngageoint/geowave/blob/302092385d841a83addcb30c120b03148dfe8a5d/extensions/datastores/accumulo/src/main/java/mil/nga/giat/geowave/datastore/accumulo/query/AccumuloFilteredIndexQuery.java#L66-L106)
5. AccumuloFilteredIndexQuery.getScanner which is actually [AccumuloQuery.getScanner](https://github.com/ngageoint/geowave/blob/302092385d841a83addcb30c120b03148dfe8a5d/extensions/datastores/accumulo/src/main/java/mil/nga/giat/geowave/datastore/accumulo/query/AccumuloQuery.java#L75-L147)
6. [AccumuloQuery.getRanges](https://github.com/ngageoint/geowave/blob/302092385d841a83addcb30c120b03148dfe8a5d/extensions/datastores/accumulo/src/main/java/mil/nga/giat/geowave/datastore/accumulo/query/AccumuloQuery.java#L65) which is actually AccumuloFilteredIndexQuery.getRanges which is actually [AccumuloConstraintsQuery.getRanges](https://github.com/ngageoint/geowave/blob/302092385d841a83addcb30c120b03148dfe8a5d/extensions/datastores/accumulo/src/main/java/mil/nga/giat/geowave/datastore/accumulo/query/AccumuloConstraintsQuery.java#L207-L210)
   - This is where the `base` object referred to above is used
7. [ConstraintsQuery.getRanges](https://github.com/ngageoint/geowave/blob/302092385d841a83addcb30c120b03148dfe8a5d/core/store/src/main/java/mil/nga/giat/geowave/core/store/query/ConstraintsQuery.java#L71-L107)
7. [DataStoreUtils.constraintsToByteArrayRanges](https://github.com/ngageoint/geowave/blob/302092385d841a83addcb30c120b03148dfe8a5d/core/store/src/main/java/mil/nga/giat/geowave/core/store/memory/DataStoreUtils.java#L73-L97)
8. [TieredSFCIndexStrategy.getQueryRanges](https://github.com/ngageoint/geowave/blob/302092385d841a83addcb30c120b03148dfe8a5d/core/index/src/main/java/mil/nga/giat/geowave/core/index/sfc/tiered/TieredSFCIndexStrategy.java#L78-L110)
9. [TieredSFCIndexStrategy.getQueryRanges](https://github.com/ngageoint/geowave/blob/302092385d841a83addcb30c120b03148dfe8a5d/core/index/src/main/java/mil/nga/giat/geowave/core/index/sfc/tiered/TieredSFCIndexStrategy.java#L112-L161)
   - Same name, different method
   - [Called once per tier](https://github.com/ngageoint/geowave/blob/302092385d841a83addcb30c120b03148dfe8a5d/core/index/src/main/java/mil/nga/giat/geowave/core/index/sfc/tiered/TieredSFCIndexStrategy.java#L103)
10. [SpaceFillingCurve.decomposeRange](https://github.com/ngageoint/geowave/blob/302092385d841a83addcb30c120b03148dfe8a5d/core/index/src/main/java/mil/nga/giat/geowave/core/index/sfc/SpaceFillingCurve.java#L68-L86) which is actually [HilbertSFC.decomposeRange](https://github.com/ngageoint/geowave/blob/302092385d841a83addcb30c120b03148dfe8a5d/core/index/src/main/java/mil/nga/giat/geowave/core/index/sfc/hilbert/HilbertSFC.java#L134-L154) which is actually [HilbertSFCOperations.decomposeRange](https://github.com/ngageoint/geowave/blob/302092385d841a83addcb30c120b03148dfe8a5d/core/index/src/main/java/mil/nga/giat/geowave/core/index/sfc/hilbert/HilbertSFCOperations.java#L90-L118) which is actually [PrimitiveHilbertSFCOperations.decomposeRange](https://github.com/ngageoint/geowave/blob/302092385d841a83addcb30c120b03148dfe8a5d/core/index/src/main/java/mil/nga/giat/geowave/core/index/sfc/hilbert/PrimitiveHilbertSFCOperations.java#L306-L436)
   - [Called once per bin](https://github.com/ngageoint/geowave/blob/302092385d841a83addcb30c120b03148dfe8a5d/core/index/src/main/java/mil/nga/giat/geowave/core/index/sfc/tiered/TieredSFCIndexStrategy.java#L136)
11. [com.google.uzaygezen.core.Query.getFilteredIndexRanges](https://github.com/aioaneid/uzaygezen/blob/34cfec2f372b55cb1dabdafc38aa3439f46a7e60/uzaygezen-core/src/main/java/com/google/uzaygezen/core/Query.java#L52-L54)

### Secondary Index ###

Secondary indices (numerical, temporal, textual, user-defined) are also available in GeoWave.
If one wishes to perform a query making use of a secondary index, it must be used [explicitly](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/test/src/test/java/mil/nga/giat/geowave/test/query/SecondaryIndexingQueryIT.java#L179-L183).
The following code

```java
final CloseableIterator<ByteArrayId> matches = secondaryIndexQueryManager.query(
                (BasicQuery) query,
                secondaryIndex,
                index,
                new String[0]);
```

produces the following chain of events

- [SecondaryIndexQueryManager.query](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/core/store/src/main/java/mil/nga/giat/geowave/core/store/index/SecondaryIndexQueryManager.java#L32-L46)
- [SecondaryIndexDataStore.query](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/core/store/src/main/java/mil/nga/giat/geowave/core/store/index/SecondaryIndexDataStore.java#L48-L53)
- [AccumuloSecondaryIndexDataStore.query](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/extensions/datastores/accumulo/src/main/java/mil/nga/giat/geowave/datastore/accumulo/index/secondary/AccumuloSecondaryIndexDataStore.java#L125-L163)

A [collection of ranges](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/extensions/datastores/accumulo/src/main/java/mil/nga/giat/geowave/datastore/accumulo/index/secondary/AccumuloSecondaryIndexDataStore.java#L140) covering the responsive records is generated using the secondary index,
and those ranges are then [scanned linearly](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/extensions/datastores/accumulo/src/main/java/mil/nga/giat/geowave/datastore/accumulo/index/secondary/AccumuloSecondaryIndexDataStore.java#L137-L139) for the desired records.

[No cost-based optimization](https://github.com/ngageoint/geowave/blob/7f1194ede7d8efd358f9f26d23dd3fc954be9ca2/core/store/src/main/java/mil/nga/giat/geowave/core/store/index/SecondaryIndexQueryManager.java#L8-L9) is done.
