# How To Run #

## Platforms ##

### Local ###

```
docker run --net=xxx -it --rm \
   -v $HOME/local/spark-2.0.0-bin-hadoop2.7:/spark:ro \
   -v $(pwd)/wave-poke/target/scala-2.11:/wave-jars:ro \
   -v $(pwd)/mesa-poke/target/scala-2.11:/mesa-jars:ro \
   -v $(pwd)/scripts:/scripts:ro \
   openjdk:8-jdk
```

- GeoMesa
   - For [jamesmcclain/geomesa:0](https://hub.docker.com/r/jamesmcclain/geomesa/)
      - `/spark/bin/spark-submit '--master=local[*]' --conf 'spark.driver.memory=8G' --class com.azavea.ca.synthetic.MesaPoke /mesa-jars/mesa-poke-assembly-0.jar instance leader root password geomesa.test point,1,uniform:-180:180,uniform:-90:90,fixed:0,1000000`
   - For [geodocker](https://github.com/geodocker/)
      - `/spark/bin/spark-submit '--master=local[*]' --conf 'spark.driver.memory=8G' --class com.azavea.ca.synthetic.MesaPoke /mesa-jars/mesa-poke-assembly-0.jar geomesa zookeeper root GisPwd geomesa.test point,1,uniform:-180:180,uniform:-90:90,fixed:0,1000000`
- GeoWave
   - For [jamesmcclain/geowave:8760ce2](https://hub.docker.com/r/jamesmcclain/geowave/)
      - `/spark/bin/spark-submit '--master=local[*]' --conf 'spark.driver.memory=8G' --class com.azavea.ca.synthetic.WavePoke /wave-jars/wave-poke-assembly-0.jar instance leader root password geowave.test space point,1,uniform:-180:180,uniform:-90:90,fixed:0,1000000`
   - For [geodocker](https://github.com/geodocker/)
      - `/spark/bin/spark-submit '--master=local[*]' --conf 'spark.driver.memory=8G' --class com.azavea.ca.synthetic.WavePoke /wave-jars/wave-poke-assembly-0.jar geowave zookeeper root GisPwd geowave.test space point,1,uniform:-180:180,uniform:-90:90,fixed:0,1000000`

### GeoDocker on EMR ###

Something like this should result in a small cluster on EMR running `quay.io/geodocker/accumulo-geowave`.
Changing the argument to the bootstrap script to `quay.io/geodocker/accumulo-geomesa` should result in a small GeoMesa setup.

```
aws emr create-cluster \
   --name "geodocker-accumulo-geowave testing" \
   --release-label emr-4.7.2 \
   --output text \
   --use-default-roles \
   --ec2-attributes KeyName=keyname \
   --applications Name=Hadoop Name=Spark Name=Zookeeper-Sandbox \
   --instance-groups \
   --region us-east-1 Name=Master,BidPrice=0.5,InstanceCount=1,InstanceGroupType=MASTER,InstanceType=m3.xlarge Name=Workers,BidPrice=0.5,InstanceCount=1,InstanceGroupType=CORE,InstanceType=m3.xlarge \
   --bootstrap-actions Name=BootstrapGeoWave,Path=s3://geotrellis-test/geodocker/bootstrap-geodocker-accumulo.sh,Args=[-i=quay.io/geodocker/accumulo-geowave:latest,-n=gis,-p=secret]
```

(In all probability, the `release-label` needs to be `emr-5.0.0` instead of `emr-4.7.2` now that this code is built with Scala 2.11, but that has not been tested.)

- GeoWave
   - `spark-submit --master yarn --deploy-mode cluster --class com.azavea.ca.synthetic.WavePoke wave-poke-assembly-0.jar gis ip-172-31-26-9 root secret geowave.test space point,100,uniform:-180:180,uniform:-90:90,fixed:0,100`
- GeoMesa
   - `spark-submit --master yarn --deploy-mode cluster --class com.azavea.ca.synthetic.MesaPoke mesa-poke-assembly-0.jar gis ip-172-31-26-9 root secret geomesa.test point,100,uniform:-180:180,uniform:-90:90,fixed:0,100`

## Arguments ##

The first five arguments to the GeoWave generator and the GeoMesa generator are the same:
   - The name of the Accumulo instance
   - The ZooKeeper(s)
   - The Accumulo username
   - The password associated with the above
   - The name of the GeoWave namespace or GeoMesa table

The sixth argument to the GeoWave generator controls the type of index that is built.
   - `space` causes a standard spatial index to be built
   - `space:xbits:ybits` causes a single-tier spatial index to be built with the given numbers of bits of precision
   - `spacetime` causes a standard spatio-temporal index to be built
   - `spacetime:xbits:ybits:tbits` causes a single-tier spatio-temporal index to be built with the given numbers of bits of precision

The remaining arguments to the GeoMesa and GeoWave generators describe geometries to generate and put into the database.
Those descriptions are discussed in the following section.

### Geometry Description ###

The argument `point,100,uniform:-180:180,uniform:-90:90,fixed:0,100` seen above is a geometry description.
One or more of these must be supplied on the command line.
They are of the form `schema,tasks,lng,lat,time,width`.
   - `schema` Must be `point`, `extent`, or `either`.
   - `tasks` is the number of (nearly) identical spark tasks to spawn for this geometry description.  If all other things are held constant, setting tasks to 3 will result in thrice as many entries in the database as there would be if it was 1.
   - `lng` is of the form `uniform:a:b`, `normal:μ:σ`, or `fixed:x`.
      - `uniform:a:b` causes the variable to be uniformly-randomly distributed between a (low) and b (high).
      - `normal:μ:σ` causes the variable to be normally distributed with mean μ and standard deviation σ.
      - `fixed:x` causes the varible to have fixed value x.
   - `lat` is also of the form `uniform:a:b`, `normal:μ:σ`, or `fixed:x`.
   - `time` is also of the form `uniform:a:b`, `normal:μ:σ`, or `fixed:x` where the number produced is interpreted as milliseconds since the epoch.
   - `width` is of the form `lo:hi:step:n` or `n`
      - `lo:hi:step:n` causes square extents to be generated with width and height ranging from `lo` to `hi` in increments of `step`, with `n` such extents at every increment.
      - `n` causes that many points to be generated.
