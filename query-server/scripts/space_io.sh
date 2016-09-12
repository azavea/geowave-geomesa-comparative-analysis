#!/usr/bin/env bash

TABLE=${1}
N=${2:-"100"}
WAVEPID=${3}
MESAPID=${4}
SCHEMA=${5:-"CommonPolygonSimpleFeatureType"}

sudo cat /proc/${WAVEPID}/io > wave.31.io
sudo cat /proc/${MESAPID}/io > mesa.31.io
for nugget in 30,0.000000168 29,0.000000335 28,0.000000671 \
                               27,0.000001341 26,0.000002682 25,0.000005364 \
                               24,0.000010729 23,0.000021458 22,0.000042915 \
                               21,0.000085831 20,0.000171661 19,0.000343323 \
                               18,0.000686646 17,0.001373291 16,0.002746582 \
                               15,0.005493164 14,0.010986328 13,0.021972656 \
                               12,0.043945313 11,0.087890625 10,0.17578125 \
                               9,0.3515625 8,0.703125 7,1.40625 \
                               6,2.8125 5,5.625 4,11.25
do
    BITS=$(echo $nugget | cut -f1 -d,)
    WIDTH=$(echo $nugget | cut -f2 -d,)
    SEED=${6:-$BITS}

    curl "http://localhost:7070/queries?width=${WIDTH}&n=${N}&seed=${SEED}&waveTable=geowave.${TABLE}&mesaTable=geomesa.${TABLE}&sftName=${SCHEMA}" > ${TABLE}.${BITS}.json
    sudo cat /proc/${WAVEPID}/io > wave.${BITS}.io
    sudo cat /proc/${MESAPID}/io > mesa.${BITS}.io
done
