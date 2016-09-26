#!/usr/bin/env bash

N=${1:-1}
M=${2:-1000000}

/spark/bin/spark-submit '--master=local[1]' \
   --conf 'spark.driver.memory=8G' \
   --class com.azavea.ca.synthetic.MesaPoke /mesa-jars/mesa-poke-assembly-0.jar \
   geomesa zookeeper root GisPwd geomesa.st_extents extent,${N},uniform:-180:180,uniform:-90:90,uniform:0:604800000,0.000343323:1:1:${M}

/spark/bin/spark-submit '--master=local[1]' \
   --conf 'spark.driver.memory=8G' \
   --class com.azavea.ca.synthetic.WavePoke /wave-jars/wave-poke-assembly-0.jar \
   geowave zookeeper root GisPwd geowave.st_extents spacetime extent,${N},uniform:-180:180,uniform:-90:90,uniform:0:604800000,0.000343323:1:1:${M}
