# Ingest notes

This document will hold notes about ingesting the various datasets for the performance tests.

## Ingesting to GeoWave

We need to look into how to use these options, and whether or not we should use them

```
-np, numPartitions
The number of partitions. Default partitions will be 1. Default: 1

-ps, --partitionStrategy
The partition strategy to use. Default will be none. Default: NONE Possible Values: [NONE, HASH, ROUND ROBIN]
```

## Ingesting into GeoMesa

GeoMesa creates z2 and z3 tables by default. Should we restrict the indexes created?

## Geolife

Based on ingests into a cluster with 5 m3.2xlarge workers.

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
_(See corrective action below)_

###### HDFS usage report

DFS Used: 34.85 GB (4.84%)

#### GeoWave - 2D index only

- Disk used: 649.73M
- Total entries: 23.80M

###### Tables

| Tables                                | Number of Entries |
| ------------------------------------- |:-----------------:|
| `geowave.geolife_GEOWAVE_METADATA`    |        190        |
| `geowave.geolife_SPATIAL_IDX`         |    23.80 M        |

###### Entries per tablet server

`257, 23.80M, 0, 0, 0`

###### HDFS usage report

- DFS Used: 4.51 GB (0.63%)

###### Action taken: descrease split size to distribute data across tablet servers

The entires per tablet server server show that all entires are on one of the 5 workers,
which will dramatically affect performance. In order to correct that,
we change the split size and compact the table. After ingest, the `table.split.threshold=1G` on `geowave.geolife_SPATIAL_IDX`

To get more splits, we execute the following command:
- `config -t geowave.geolife_SPATIAL_IDX -s table.split.threshold=128M`
- `compact -t geowave.geolife_SPATIAL_IDX`

This gave the following entries per table:

`5.83M, 5.85M, 2.91M, 2.87M, 5.88M`

#### GeoWave - 2D and 3D

- Disk Used: 1.11G
- Total Entries: 47.24M

###### Tables

| Tables                                                 | Number of Entries |
| -------------------------------------                  |:-----------------:|
| `geowave.geolife3D_GEOWAVE_METADATA`                   |        11         |
| `geowave.geolife3D_SPATIAL_TEMPORAL_IDX_BALANCED_YEAR` |      23.44M       |
| `geowave.geolife_GEOWAVE_METADATA`                     |        11         |
| `geowave.geolife_SPATIAL_IDX`                          |      23.80 M      |

###### Entries per tablet server

`23.80M, 0, 11, 12, 23.44`

###### HDFS usage report

- DFS Used: 9.23 GB (1.28%)

###### Action taken: descrease split size to distribute data across tablet servers

The entires per tablet server server show that all entires are on one of the 5 workers,
which will dramatically affect performance. In order to correct that,
we change the split size and compact the table. After ingest, the `table.split.threshold=1G` on `geowave.geolife_SPATIAL_IDX`

To get more splits, we execute the following command:
- `config -t geowave.geolife_SPATIAL_IDX -s table.split.threshold=128M`
- `compact -t geowave.geolife_SPATIAL_IDX`
- `config -t geowave.geolife3D_SPATIAL_TEMPORAL_IDX_BALANCED_YEAR -s table.split.threshold=128M`
- `compact -t geowave.geolife3D_SPATIAL_TEMPORAL_IDX_BALANCED_YEAR`

- `config -t geowave.geolife3Dp_SPATIAL_TEMPORAL_IDX_BALANCED_YEAR_POINTONLY -s table.split.threshold=200M`
- `compact -t geowave.geolife3Dp_SPATIAL_TEMPORAL_IDX_BALANCED_YEAR_POINTONLY`
- `config -t geowave.geolife3Dp_SPATIAL_TEMPORAL_IDX_BALANCED_YEAR_POINTONLY -s table.split.threshold=100M`
This gave the following entries per table:

`14.61M,2.88M,8.82M,8.78M,11.59M`
