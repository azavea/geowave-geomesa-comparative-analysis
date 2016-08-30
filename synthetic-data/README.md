# How To Run #

## Platforms ##

### Locally ###

```
docker run --net=xxx -it --rm \
   -v $HOME/local/spark-2.0.0-bin-hadoop2.7:/spark:ro \
   -v $(pwd)/wave-poke/target/scala-2.11:/wave-jars:ro \
   -v $(pwd)/mesa-poke/target/scala-2.11:/mesa-jars:ro \
   -v $(pwd)/scripts:/scripts:ro \
   openjdk:8-jdk
```

- GeoMesa
   - For jamesmcclain/geomesa:0
      - `/spark/bin/spark-submit '--master=local[*]' --conf 'spark.driver.memory=8G' --class com.azavea.geomesa.MesaPoke /mesa-jars/mesa-poke-assembly-0.jar instance leader root password geomesa.test point,1,uniform:-180:180,uniform:-90:90,fixed:0,1000000`
   - For GeoDocker
      - `/spark/bin/spark-submit '--master=local[*]' --conf 'spark.driver.memory=8G' --class com.azavea.geomesa.MesaPoke /mesa-jars/mesa-poke-assembly-0.jar geomesa zookeeper root GisPwd geomesa.test point,1,uniform:-180:180,uniform:-90:90,fixed:0,1000000`
- GeoWave
   - For jamesmcclain/geowave:7
      - `/spark/bin/spark-submit '--master=local[*]' --conf 'spark.driver.memory=8G' --class com.azavea.geowave.WavePoke /wave-jars/wave-poke-assembly-0.jar instance leader root password geowave.test space point,1,uniform:-180:180,uniform:-90:90,fixed:0,1000000`
   - For GeoDocker
      - `/spark/bin/spark-submit '--master=local[*]' --conf 'spark.driver.memory=8G' --class com.azavea.geowave.WavePoke /wave-jars/wave-poke-assembly-0.jar geowave zookeeper root GisPwd geowave.test space point,1,uniform:-180:180,uniform:-90:90,fixed:0,1000000`

### GeoDocker on EMR ###

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
   --bootstrap-actions Name=BootstrapGeoWave,Path=s3://geotrellis-test/geodocker/bootstrap-geodocker-accumulo.sh,Args=[-i=quay.io/geodocker/accumulo-geomesa:latest,-n=gis,-p=secret]
```

(In all probability, the `release-label` needs to be `emr-5.0.0` instead of `emr-4.7.2` now that this code is built with Scala 2.11, but that has not been tested.)

- GeoWave
   - `spark-submit --master yarn --deploy-mode cluster --class com.azavea.geowave.WavePoke wave-poke-assembly-0.jar gis ip-172-31-26-9 root secret geowave.test stock point,100,uniform:-180:180,uniform:-90:90,fixed:0,100`
- GeoMesa
   - `spark-submit --master yarn --deploy-mode cluster --class com.azavea.geomesa.MesaPoke mesa-poke-assembly-0.jar gis ip-172-31-26-9 root secret geomesa.test point,100,uniform:-180:180,uniform:-90:90,fixed:0,100`

## Geometry Description ##

The argument `point,100,uniform:-180:180,uniform:-90:90,fixed:0,100` seen above is a geometry description.
One or more of these must be supplied on the command line.
They are of the form `schema,tasks,lng,lat,time,width`.
   - `schema` Must be `point`, `extent`, or `either`.
   - `tasks` is the number of identical spark tasks to spawn for this geometry.  If all other things are held constant, setting tasks to 3 you will result in thrice as many entries in the database as there would be if it was 1.
   - `lng` is of the form `uniform:a:b`, `normal:μ:σ`, or `fixed:x`.
      - `uniform:a:b` causes the variable to be uniformly randomly distributed between a (low) and b (high).
      - `normal:μ:σ` causes the variable to be normally distributed with mean μ and standard deviation σ.
      - `fixed:x` causes the arible to have fixed value x.
   - `lat` is also of the form `uniform:a:b`, `normal:μ:σ`, or `fixed:x`.
   - `time` is also of the form `uniform:a:b`, `normal:μ:σ`, or `fixed:x` where the number produced interpreted as milliseconds since the epoch.
   - `width` is of the form `lo:hi:step:n` or `n`
      - `lo:hi:step:n` causes square extents to be generated with width and height ranging from `lo` to `hi` in increments of `step`, with `n` such extents at every increment.
      - `n` causes that many points to be generated.

