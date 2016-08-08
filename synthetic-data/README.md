### How To Run ###

GeoMesa
   1. Start a container
      - docker run -it --rm -v $SPARK_HOME:/spark:ro -v $(pwd)/synthetic-data/mesa-poke/target/scala-2.10:/jars:ro java:openjdk-8u72-jdk
   2. Run within the container
      - /spark/bin/spark-submit '--master=local[*]' --class com.azavea.geomesa.MesaPoke /jars/mesa-poke-assembly-0.jar instance leader root password GeoMesa point:true:10:10
      - /spark/bin/spark-submit '--master=local[*]' --class com.azavea.geomesa.MesaPoke /jars/mesa-poke-assembly-0.jar geowave zookeeper root GisPwd geomesa.test point:true:10:10

GeoWave
   1. Start a container
      - docker run -it --rm -v $SPARK_HOME:/spark:ro -v $(pwd)/synthetic-data/wave-poke/target/scala-2.10:/jars:ro java:openjdk-8u72-jdk
   2. Run within the container
      - /spark/bin/spark-submit '--master=local[*]' --class com.azavea.geowave.WavePoke /jars/wave-poke-assembly-0.jar instance leader root password vector point:true:10:10
      - /spark/bin/spark-submit '--master=local[*]' --class com.azavea.geowave.WavePoke /jars/wave-poke-assembly-0.jar geomesa zookeeper root GisPwd geowave.test point:true:10:10
