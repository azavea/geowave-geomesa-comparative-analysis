#!/usr/bin/env bash

# This script wraps the awkward emr spark-submit API.
# Once the cluster ID and step name is provided the rest of the arguments are the normal spark-submit command as it would be run on EMR
# That means the JAR must be an S3 URI and --master must be yarn-cluster
# Using this wrapper saves a host of errors caused by converting spaces to commas

CLUSTER_ID=$1
NAME=$2
shift 2
read -a ARGS <<< $(printf "\"%s\" " "$@")
IFS=','
aws emr add-steps --output text --cluster-id ${CLUSTER_ID} \
--steps Name="${NAME}",Type=CUSTOM_JAR,Jar=command-runner.jar,Args=["${ARGS[*]}"]
