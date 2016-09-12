#!/usr/bin/env bash

SYSTEM=${1}
TABLE=${2}
FILENAME=${SYSTEM}.csv

for i in $(seq 4 30)
do
    jq "map(.${SYSTEM}.time)" ${TABLE}.${i}.json | grep '[[:digit:]]' | tr '\n' ' ' | sed 's, ,,g' >> ${FILENAME}
    echo >> ${FILENAME}
done
