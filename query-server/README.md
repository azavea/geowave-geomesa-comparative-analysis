# GeoWave/GeoMesa Query Server

## Server

This repository stores the source files and scripts necessary to
build a containerized bastion into geowave and geomesa clusters. Its
primary purpose is to carry out and analyze the performance of queries
over each of these frameworks.

### Building and testing

To build, simply run `make build`. Scala source will be compiled and a
docker image will be generated which serves this (akka-http backed)
project on port 7070.

### Running ###

`docker-compose up`

## Explore

The `explore` project is meant to allow for interactive exploration of the datasets within the spark shell. This is meant to be added as a JAR on the spark shell on the GeoMesa EMR master instance.

To use, run `make explore` in the `deployment` directory. This will ssh you into the EMR master instance of the geomesa EMR cluster.
Then, run the `run-explore-shell.sh` script to start your spark shell.

### Examples

You can run these in the spark shell that is started by `run-explore-shell.sh`

```scala
import com.azavea.ca.explore.Geolife

Geolife.run
```
