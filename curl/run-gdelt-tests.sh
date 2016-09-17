#!/bin/bash

COUNTER=0
while [  $COUNTER -lt 100 ]; do
    echo "RUN $COUNTER"
    python gdelt-france-regions-tests.py | tee gdelt-france-output-${COUNTER}.txt
    # python gdelt-sa-tests.py | tee gdelt-france-output-${COUNTER}.txt
    # python gdelt-france-tests.py | tee gdelt-france-output-${COUNTER}.txt
    # python gdelt-russia-tests.py | tee gdelt-russia-output-${COUNTER}.txt
    let COUNTER=COUNTER+1
done
