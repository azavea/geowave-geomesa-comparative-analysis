#!/bin/bash

export GW_CLUSTER_ID=gw_cluster_id
export GW_ZK=zookeeper
export GW_USER=root
export GW_PASS=GisPwd
export GW_INSTANCE=geowave
export GM_CLUSTER_ID=gm_cluster_id
export GM_ZK=zookeeper
export GM_USER=root
export GM_PASS=GisPwd
export GM_INSTANCE=geomesa

./sbt "project server" ~reStart
