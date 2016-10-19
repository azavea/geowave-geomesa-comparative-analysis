# Ingest notes

This document will hold notes about ingesting the various datasets for the performance tests.

## Geolife

Based on ingests into a cluster with 5 m3.2xlarge workers.

_Note: The performance test were performed against a 3 node cluster with a similar setup._

#### GeoMesa

- Disk Used:      1.68G
- Total Entries: 71.59M

###### Tables

| Tables                                | Number of Entries |
| ------------------------------------- |:-----------------:|
| `geomesa.geolife`                     |        10         |
| `geomesa.geolife_gmtrajectory_z3`     |    24.60 M        |
| `geomesa.geolife_records`             |    24.35 M        |
| `geomesa.geolife_stats`               |     8.00 K        |
| `geomesa.geolife_z2`                  |    24.55 M        |

###### Entries per tablet server

`11.95M, 11.67M, 11.67M, 11.95M, 24.35M`

###### HDFS usage report

DFS Used: 34.85 GB (4.84%)

#### GeoWave

- Disk Used: 1.45G
- Total Entries: 47.24M

###### Tables

| Tables                                                         | Number of Entries |
| -------------------------------------                          |:-----------------:|
| `geowave.geolife_SPATIAL_TEMPORAL_IDX_BALANCED_YEAR_POINTONLY` |      23.44M       |
| `geowave.geolife_GEOWAVE_METADATA`                             |        30         |
| `geowave.geolife_SPATIAL_IDX`                                  |      23.82 M      |

###### Entries per tablet server

The entires per tablet server server show that all entires are on one of the 5 workers,
which will dramatically affect performance. In order to correct that,
we change the split size and compact the table.

To get more splits, we execute the following command:

```
config -t geowave.geolife_SPATIAL_IDX -s table.split.threshold=100M
compact -t geowave.geolife_SPATIAL_IDX
config -t geowave.geolife_SPATIAL_TEMPORAL_IDX_BALANCED_YEAR_POINTONLY -s table.split.threshold=100M
compact -t geowave.geolife_SPATIAL_TEMPORAL_IDX_BALANCED_YEAR_POINTONLY
```

This gave the following entries per table:

`14.57M,8.81M,8.70M,2.92M,11.67M`


###### HDFS usage report

- DFS Used: 12.5 GB (1.74%)


## GDELT

Based on ingests into a cluster with 5 m3.2xlarge workers.
These stats were taken after ingest completed and compaction was done to all tables containing many entries.

#### GeoMesa

- Disk Used:       98.75G
- Total Entries:    1.22B

###### Tables

| Tables                                | Number of Entries |
| ------------------------------------- |:-----------------:|
| `geomesa.gdelt`                       |        10         |
| `geomesa.gdelt_gdelt_2devent_z3 `     |    406.51M        |
| `geomesa.gdelt_records`               |    406.51M        |
| `geomesa.gdelt_stats`                 |      7.88K        |
| `geomesa.gdelt_z2`                    |    406.51M        |

###### Tablet servers

| Tablet Count  | Number of Entries |
| ------------- |:-----------------:|
|      47       |    242.86M        |
|      44       |    234.28M        |
|      48       |    237.68M        |
|      46       |    241.10M        |
|      46       |    263.62M        |


###### HDFS usage report

DFS Used: 202.61 GB (28.16%)

#### GeoWave

We had problems ingesting GDELT, where the `geowave.gdelt_GEOWAVE_METADATA` table had way too many entries, all stored to memory,
and never flushing to disk although there was one minor compaction running the whole time. Any query or compact command
to that table would hang and timeout. We got around this issue by not saving any statistics to the table, by using the
`AccumuloOptions.setPersistDataStatistics(false)` method for our datastore options. An attempt was made to use the
`recalcstats` command in the geowave geotools, however we were unable to get this to work.

- Disk Used: 73.81
- Total Entries: 813.19

###### Tables

| Tables                                                              | Number of Entries |
| -------------------------------------                               |:-----------------:|
| `geowave.gdelt_SPATIAL_TEMPORAL_IDX_BALANCED_WEEK_HASH_4_POINTONLY` |      406.60M      |
| `geowave.gdelt_GEOWAVE_METADATA`                                    |           4       |
| `geowave.gdelt_SPATIAL_IDX_HASH_4`                                  |      406.60M      |

###### Entries per tablet server

| Tablet Count  | Number of Entries |
| ------------- |:-----------------:|
|      28       |    166.40M        |
|      26       |    151.95M        |
|      27       |    158.78M        |
|      29       |    170.14M        |
|      29       |    165.92M        |

###### HDFS usage report

- 156.6 GB (21.76%)

## Tracks

Based on ingests into a cluster with 5 m3.2xlarge workers.
These stats were pulled from a cluster that had undergone extensive performance testing.

#### GeoMesa

- Disk Used:       58.12G
- Total Entries:   19.59M

###### Tables

| Tables                                | Number of Entries |
|-------------------------------------- |:-----------------:|
| `geomesa.tracks`                      |        10         |
| `geomesa.tracks_records`              |      6.41M        |
| `geomesa.tracks_stats`                |        68         |
| `geomesa.tracks_xz2`                  |      6.33M        |
| `geomesa.tracks_xz3 `                 |      6.57M        |

###### Tablet servers

| Tablet Count  | Number of Entries |
| ------------- |:-----------------:|
|      45       |      4.22M        |
|      43       |      3.79M        |
|      45       |      3.66M        |
|      47       |      3.80M        |
|      44       |      4.13M        |


###### HDFS usage report

DFS Used: 120.6 GB (16.76%)

#### GeoWave

There is more entries here, which can be explained by the fact that GeoWave can store up to 3 duplicates per entry based on their indexing scheme.

- Disk Used: 106.24G
- Total Entries: 35.83M

###### Tables

| Tables                                                              | Number of Entries |
| ------------------------------------------------------------------  |:-----------------:|
| `geowave.tracks_SPATIAL_TEMPORAL_IDX_BALANCED_YEAR`                 |      18.04M       |
| `geowave.tracks_GEOWAVE_METADATA`                                   |          38       |
| `geowave.gdelt_SPATIAL_IDX_HASH_4`                                  |      17.78M       |

###### Entries per tablet server

| Tablet Count  | Number of Entries |
| ------------- |:-----------------:|
|      37       |      7.37M        |
|      41       |      6.59M        |
|      39       |      5.75M        |
|      37       |      6.26M        |
|      37       |      9.85M        |

###### HDFS usage report

- 218.93 GB (30.43%)
