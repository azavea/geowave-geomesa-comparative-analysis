# Generated Tracks Dataset


## Setup

Master: `m3.xlarge`
Workers: 3 x `m3.2xlarge`

### Test Key Structure

`TRACKS-USA-GRID-{CONTEXT}-{TIME_STEP}-{TIME_INDEX}-{ZOOM_LEVEL}-{COL}-{ROW}`

 - `CONTEXT`: System state context, number of user, server, workers, request profile
 - `TIME_STEP`: Size of time window in the query (ex: 5 days, 7 days, 1 month ,etc)
 - `TIME_INDEX`: Index of the time window used for the query, starting from 0 to max index that fits in a year
 - `ZOOM_LEVEL`: Level of spatial selectivity, lower number implies larger area
 - `COL`: Column in the extent layout for the `ZOOM_LEVEL`
 - `ROW`: Row in the extent layout for the `ZOOM_LEVEL`

This query schemes divides the are of interest, Continental USA into grid of extends `2^ZOOM_LEVEL` columns wide and `2^(ZOOM_LEVEL-1)` rows wide.
Along with time resolution each query represents a non intersecting cube within the data space.
The benchmark client is going to walk psudo-random path through this space and record results for each query.

### GeoWave

| Table                                             | Tablets | Entries
|---------------------------------------------------|---------|---------
| geowave.tracks_SPATIAL_IDX                        | 341     | 29.35M
| geowave.tracks_SPATIAL_TEMPORAL_IDX_BALANCED_YEAR | 295     | 30.98M

| Property               | Value
|------------------------|---------------
| tablet.split.threshold | 512M
| DFS Used               | 192.31 GB (44.54%)

### GeoMesa

| Table                  | Tablets | Entries
|------------------------|---------|-------
| geomesa.tracks_records | 66      | 6.31M
| geomesa.tracks_xz2     | 60      | 6.31M
| geomesa.tracks_xz3     | 62      | 6.49M

| Property               | Value
|------------------------|---------------
| tablet.split.threshold | 512M
| DFS Used               | 59.31 GB (13.74%)


## Result Set: `SET1`

Query Service: 1 x `m3.xlarge`
Query Profile: 1 user at a time, over 10 minutes

### GeoMesa

`TRACKS-USA-GRID-SET1-5DAY`

```
================================================================================
---- Global Information --------------------------------------------------------
> request count                                         49 (OK=49     KO=0     )
> min response time                                    338 (OK=338    KO=-     )
> max response time                                   9924 (OK=9924   KO=-     )
> mean response time                                  2509 (OK=2509   KO=-     )
> std deviation                                       3217 (OK=3217   KO=-     )
> response time 50th percentile                        577 (OK=577    KO=-     )
> response time 75th percentile                       6731 (OK=6731   KO=-     )
> response time 95th percentile                       8346 (OK=8346   KO=-     )
> response time 99th percentile                       9895 (OK=9895   KO=-     )
> mean requests/sec                                  0.392 (OK=0.392  KO=-     )
---- Response Time Distribution ------------------------------------------------
> t < 800 ms                                            34 ( 69%)
> 800 ms < t < 1200 ms                                   1 (  2%)
> t > 1200 ms                                           14 ( 29%)
> failed                                                 0 (  0%)
================================================================================
```

log: `/Users/eugene/proj/geowave-geomesa-comparative-analysis/benchmarks-client/target/gatling/tracksgrid-1474397103629/index.html`

### GeoWave

`TRACKS-USA-GRID-SET1-5DAY`

```
================================================================================
---- Global Information --------------------------------------------------------
> request count                                         64 (OK=64     KO=0     )
> min response time                                    245 (OK=245    KO=-     )
> max response time                                   5361 (OK=5361   KO=-     )
> mean response time                                  1854 (OK=1854   KO=-     )
> std deviation                                       1180 (OK=1180   KO=-     )
> response time 50th percentile                       1755 (OK=1755   KO=-     )
> response time 75th percentile                       2373 (OK=2373   KO=-     )
> response time 95th percentile                       3786 (OK=3786   KO=-     )
> response time 99th percentile                       4754 (OK=4754   KO=-     )
> mean requests/sec                                  0.529 (OK=0.529  KO=-     )
---- Response Time Distribution ------------------------------------------------
> t < 800 ms                                            15 ( 23%)
> 800 ms < t < 1200 ms                                   2 (  3%)
> t > 1200 ms                                           47 ( 73%)
> failed                                                 0 (  0%)
================================================================================
```

log: `/Users/eugene/proj/geowave-geomesa-comparative-analysis/benchmarks-client/target/gatling/tracksgrid-1474397687722/index.html`
