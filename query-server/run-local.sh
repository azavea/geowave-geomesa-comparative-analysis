docker run -it --net=development_default --rm -p 7070:7070 \
   -e GM_CLUSTER_ID=gm_cluster_id -e GM_USER=root -e GM_PASS=GisPwd -e GM_INSTANCE=geomesa -e GM_ZK=zookeeper \
   -e GW_CLUSTER_ID=gw_cluster_id -e GW_USER=root -e GW_PASS=GisPwd -e GW_INSTANCE=geowave -e GW_ZK=zookeeper \
   -v $(pwd):/code:rw \
   -v $HOME/.aws:/root/.aws:rw \
   -v $HOME/.ivy2:/root/.ivy2:rw \
   -v $HOME/.m2:/root/.m2:rw \
   -v $HOME/.sbt:/root/.sbt:rw openjdk:8-jdk
