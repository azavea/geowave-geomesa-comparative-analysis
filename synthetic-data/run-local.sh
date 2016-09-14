docker run --net=development_default -it --rm \
   -v $HOME/bin/spark-2.0.0-bin-hadoop2.7:/spark:ro \
   -v $(pwd)/wave-poke/target/scala-2.11:/wave-jars:ro \
   -v $(pwd)/mesa-poke/target/scala-2.11:/mesa-jars:ro \
   -v $(pwd)/scripts:/scripts:ro \
   openjdk:8-jdk
