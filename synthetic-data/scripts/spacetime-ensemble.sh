#!/usr/bin/env bash

N=${1:-1}
M=${2:-1000000}

/spark/bin/spark-submit '--master=local[1]' \
   --conf 'spark.driver.memory=8G' \
   --class com.azavea.geomesa.MesaPoke /mesa-jars/mesa-poke-assembly-0.jar \
   geomesa zookeeper root GisPwd geomesa.space_ensemble \
   extent,${N},uniform:-180:180,uniform:-90:90,uniform:0:604800000,0.000000168:1:1:${M} \
   extent,${N},uniform:-180:180,uniform:-90:90,uniform:0:604800000,0.000000335:1:1:${M} \
   extent,${N},uniform:-180:180,uniform:-90:90,uniform:0:604800000,0.000000671:1:1:${M} \
   extent,${N},uniform:-180:180,uniform:-90:90,uniform:0:604800000,0.000001341:1:1:${M} \
   extent,${N},uniform:-180:180,uniform:-90:90,uniform:0:604800000,0.000002682:1:1:${M} \
   extent,${N},uniform:-180:180,uniform:-90:90,uniform:0:604800000,0.000005364:1:1:${M} \
   extent,${N},uniform:-180:180,uniform:-90:90,uniform:0:604800000,0.000010729:1:1:${M} \
   extent,${N},uniform:-180:180,uniform:-90:90,uniform:0:604800000,0.000021458:1:1:${M} \
   extent,${N},uniform:-180:180,uniform:-90:90,uniform:0:604800000,0.000042915:1:1:${M} \
   extent,${N},uniform:-180:180,uniform:-90:90,uniform:0:604800000,0.000085831:1:1:${M} \
   extent,${N},uniform:-180:180,uniform:-90:90,uniform:0:604800000,0.000171661:1:1:${M} \
   extent,${N},uniform:-180:180,uniform:-90:90,uniform:0:604800000,0.000343323:1:1:${M}

/spark/bin/spark-submit '--master=local[1]' \
   --conf 'spark.driver.memory=8G' \
   --class com.azavea.geowave.WavePoke /wave-jars/wave-poke-assembly-0.jar \
   geowave zookeeper root GisPwd geowave.space_ensemble spacetime \
   extent,${N},uniform:-180:180,uniform:-90:90,uniform:0:604800000,0.000000168:1:1:${M} \
   extent,${N},uniform:-180:180,uniform:-90:90,uniform:0:604800000,0.000000335:1:1:${M} \
   extent,${N},uniform:-180:180,uniform:-90:90,uniform:0:604800000,0.000000671:1:1:${M} \
   extent,${N},uniform:-180:180,uniform:-90:90,uniform:0:604800000,0.000001341:1:1:${M} \
   extent,${N},uniform:-180:180,uniform:-90:90,uniform:0:604800000,0.000002682:1:1:${M} \
   extent,${N},uniform:-180:180,uniform:-90:90,uniform:0:604800000,0.000005364:1:1:${M} \
   extent,${N},uniform:-180:180,uniform:-90:90,uniform:0:604800000,0.000010729:1:1:${M} \
   extent,${N},uniform:-180:180,uniform:-90:90,uniform:0:604800000,0.000021458:1:1:${M} \
   extent,${N},uniform:-180:180,uniform:-90:90,uniform:0:604800000,0.000042915:1:1:${M} \
   extent,${N},uniform:-180:180,uniform:-90:90,uniform:0:604800000,0.000085831:1:1:${M} \
   extent,${N},uniform:-180:180,uniform:-90:90,uniform:0:604800000,0.000171661:1:1:${M} \
   extent,${N},uniform:-180:180,uniform:-90:90,uniform:0:604800000,0.000343323:1:1:${M}
