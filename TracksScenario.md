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

#### `TRACKS-USA-GRID-SET1-5DAY`, `ZOOM_LEVEL=4`

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

#### `TRACKS-USA-GRID-SET1-5DAY`, `ZOOM_LEVEL=5`

```
================================================================================
---- Global Information --------------------------------------------------------
> request count                                         54 (OK=54     KO=0     )
> min response time                                    221 (OK=221    KO=-     )
> max response time                                   9590 (OK=9590   KO=-     )
> mean response time                                  2236 (OK=2236   KO=-     )
> std deviation                                       3061 (OK=3061   KO=-     )
> response time 50th percentile                        487 (OK=487    KO=-     )
> response time 75th percentile                       5263 (OK=5263   KO=-     )
> response time 95th percentile                       7794 (OK=7794   KO=-     )
> response time 99th percentile                       9160 (OK=9160   KO=-     )
> mean requests/sec                                  0.439 (OK=0.439  KO=-     )
---- Response Time Distribution ------------------------------------------------
> t < 800 ms                                            39 ( 72%)
> 800 ms < t < 1200 ms                                   1 (  2%)
> t > 1200 ms                                           14 ( 26%)
> failed                                                 0 (  0%)
================================================================================
```

log: `/Users/eugene/proj/geowave-geomesa-comparative-analysis/benchmarks-client/target/gatling/tracksgrid-1474398538135/index.html`

#### `TRACKS-USA-GRID-SET1-5DAY`, `ZOOM_LEVEL=6`

```
================================================================================
---- Global Information --------------------------------------------------------
> request count                                        196 (OK=196    KO=0     )
> min response time                                    239 (OK=239    KO=-     )
> max response time                                  25154 (OK=25154  KO=-     )
> mean response time                                  2441 (OK=2441   KO=-     )
> std deviation                                       3813 (OK=3813   KO=-     )
> response time 50th percentile                        486 (OK=486    KO=-     )
> response time 75th percentile                       2630 (OK=2630   KO=-     )
> response time 95th percentile                       8706 (OK=8706   KO=-     )
> response time 99th percentile                      10613 (OK=10613  KO=-     )
> mean requests/sec                                  0.405 (OK=0.405  KO=-     )
---- Response Time Distribution ------------------------------------------------
> t < 800 ms                                           143 ( 73%)
> 800 ms < t < 1200 ms                                   3 (  2%)
> t > 1200 ms                                           50 ( 26%)
> failed                                                 0 (  0%)
================================================================================
```

log: `/Users/eugene/proj/geowave-geomesa-comparative-analysis/benchmarks-client/target/gatling/tracksgrid-1474399155062/index.html`

### GeoWave

#### `TRACKS-USA-GRID-SET1-5DAY`, `ZOOM_LEVEL=4`

```
================================================================================
---- Global Information --------------------------------------------------------
> request count                                         64 (OK=64     KO=0     )
> min response time                                    245 (OK=245    KO=-     )
> max response time                                   5361 (OK=5361   KO=-     )
> mean response time                                  1854 (OK=1854   KO=-     )
> std deviation                                       1180 (OK=1180   KO=-     )
> response time 50th percentile                       1755 (OK=1755   KO=-     )
> response time 75th percentile                        2373 (OK=2373   KO=-     )
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

#### `TRACKS-USA-GRID-SET1-5DAY`, `ZOOM_LEVEL=5`

```
================================================================================
---- Global Information --------------------------------------------------------
> request count                                         73 (OK=73     KO=0     )
> min response time                                    167 (OK=167    KO=-     )
> max response time                                   3748 (OK=3748   KO=-     )
> mean response time                                  1616 (OK=1616   KO=-     )
> std deviation                                       1027 (OK=1027   KO=-     )
> response time 50th percentile                       1645 (OK=1645   KO=-     )
> response time 75th percentile                       2157 (OK=2157   KO=-     )
> response time 95th percentile                       3388 (OK=3388   KO=-     )
> response time 99th percentile                       3663 (OK=3663   KO=-     )
> mean requests/sec                                  0.603 (OK=0.603  KO=-     )
---- Response Time Distribution ------------------------------------------------
> t < 800 ms                                            19 ( 26%)
> 800 ms < t < 1200 ms                                   5 (  7%)
> t > 1200 ms                                           49 ( 67%)
> failed                                                 0 (  0%)
================================================================================
```

log: `/Users/eugene/proj/geowave-geomesa-comparative-analysis/benchmarks-client/target/gatling/tracksgrid-1474398799139/index.html`

#### `TRACKS-USA-GRID-SET1-5DAY`, `ZOOM_LEVEL=6`

```
================================================================================
---- Global Information --------------------------------------------------------
> request count                                        308 (OK=308    KO=0     )
> min response time                                    109 (OK=109    KO=-     )
> max response time                                   5606 (OK=5606   KO=-     )
> mean response time                                  1539 (OK=1539   KO=-     )
> std deviation                                       1057 (OK=1057   KO=-     )
> response time 50th percentile                       1567 (OK=1567   KO=-     )
> response time 75th percentile                       2148 (OK=2148   KO=-     )
> response time 95th percentile                       3235 (OK=3235   KO=-     )
> response time 99th percentile                       4249 (OK=4249   KO=-     )
> mean requests/sec                                  0.638 (OK=0.638  KO=-     )
---- Response Time Distribution ------------------------------------------------
> t < 800 ms                                            78 ( 25%)
> 800 ms < t < 1200 ms                                  44 ( 14%)
> t > 1200 ms                                          186 ( 60%)
> failed                                                 0 (  0%)
================================================================================
```

logs: `/Users/eugene/proj/geowave-geomesa-comparative-analysis/benchmarks/target/gatling/tracksgrid-1474399964762/index.html`
