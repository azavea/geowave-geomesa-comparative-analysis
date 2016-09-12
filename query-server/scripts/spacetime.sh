#!/usr/bin/env bash

TABLE=${1}
N=${2:-"500"}
SCHEMA=${3:-"CommonPolygonSimpleFeatureType"}

for nugget in 30,0.000000168,1 29,0.000000335,1 28,0.000000671,1 \
                               27,0.000001341,1 26,0.000002682,1 25,0.000005364,1 \
                               24,0.000010729,1 23,0.000021458,1 22,0.000042915,1 \
                               21,0.000085831,1 20,0.000171661,1 19,0.000343323,1 \
                               18,0.000686646,1 17,0.001373291,1 16,0.002746582,1 \
                               15,0.005493164,2 14,0.010986328,2 13,0.021972656,2 \
                               12,0.043945313,2 11,0.087890625,2 10,0.17578125,2 \
                               9,0.3515625,2 8,0.703125,2 7,1.40625,2 \
                               6,2.8125,2 5,5.625,2 4,11.25,2
do
    BITS=$(echo $nugget | cut -f1 -d,)
    WIDTH=$(echo $nugget | cut -f2 -d,)
    D=$(echo $nugget | cut -f3 -d,)
    NUM=$(expr $N / $D)
    SEED=${4:-$BITS}

    # curl "http://localhost:7070/queries?width=${WIDTH}&n=${NUM}&seed=${SEED}&waveTable=geowave.${TABLE}&mesaTable=geomesa.${TABLE}&sftName=${SCHEMA}&from=1970-01-03T00:00:00.000Z&to=1970-01-04T00:00:00.000Z" > ${TABLE}.${BITS}.json

    curl "http://localhost:7070/queries?width=${WIDTH}&n=${NUM}&seed=${SEED}&waveTable=geowave.${TABLE}&sftName=${SCHEMA}&from=1970-01-03T00:00:00.000Z&to=1970-01-04T00:00:00.000Z" > ${TABLE}.${BITS}.json
done
