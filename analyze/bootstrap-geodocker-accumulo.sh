#!/usr/bin/env bash
#
# Bootstrap docker and accumulo on EMR cluster
#

IMAGE=quay.io/geodocker/accumulo:${TAG:-"latest"}
ACCUMULO_SECRET=DEFAULT
ACCUMULO_PASSWORD=secret
INSTANCE_NAME=accumulo
ARGS=$@

for i in "$@"
do
    case $i in
        --continue)
            CONTINUE=true
            shift
            ;;
        -i=*|--image=*)
            IMAGE="${i#*=}"
            shift
            ;;
        -s=*|--accumulo-secret=*)
            ACCUMULO_SECRET="${i#*=}"
            shift
            ;;
        -p=*|--accumulo-password=*)
            ACCUMULO_PASSWORD="${i#*=}"
            shift
            ;;
        -n=*|--instance-name=*)
            INSTANCE_NAME="${i#*=}"
            shift
            ;;

        *)
            ;;
    esac
done

# Parses a configuration file put in place by EMR to determine the role of this node
is_master() {
  if [ $(jq '.isMaster' /mnt/var/lib/info/instance.json) = 'true' ]; then
    return 0
  else
    return 1
  fi
}

### MAIN ####

# EMR bootstrap runs before HDFS or YARN are initilized
if [ ! $CONTINUE ]; then
    sudo yum -y install docker
    sudo usermod -aG docker hadoop
    sudo service docker start

    THIS_SCRIPT="$(realpath "${BASH_SOURCE[0]}")"
	  TIMEOUT= is_master && TIMEOUT=3 || TIMEOUT=4
	  echo "bash -x $THIS_SCRIPT --continue $ARGS > /tmp/geodocker-accumulo-bootstrap.log" | at now + $TIMEOUT min
	  exit 0 # Bail and let EMR finish initializing
fi

YARN_RM=$(xmllint --xpath "//property[name='yarn.resourcemanager.hostname']/value/text()"  /etc/hadoop/conf/yarn-site.xml)
DOCKER_ENV="-e USER=hadoop \
--e HADOOP_MASTER_ADDRESS=$YARN_RM \
-e ZOOKEEPERS=$YARN_RM \
-e ACCUMULO_SECRET=$ACCUMULO_SECRET \
-e ACCUMULO_PASSWORD=$ACCUMULO_PASSWORD \
--e INSTANCE_NAME=$INSTANCE_NAME"

DOCKER_OPT="-d --net=host --restart=always"
if is_master ; then
    docker run $DOCKER_OPT --name=accumulo-master $DOCKER_ENV $IMAGE master --auto-init
    docker run $DOCKER_OPT --name=accumulo-monitor $DOCKER_ENV $IMAGE monitor
    docker run $DOCKER_OPT --name=accumulo-tracer $DOCKER_ENV $IMAGE tracer
    docker run $DOCKER_OPT --name=accumulo-gc $DOCKER_ENV $IMAGE gc
    docker run $DOCKER_OPT --name=geoserver quay.io/geodocker/geoserver:latest
else # is worker
    docker run -d --net=host --name=accumulo-tserver $DOCKER_ENV $IMAGE tserver
fi
