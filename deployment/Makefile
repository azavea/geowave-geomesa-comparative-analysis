# No spaces in STACK_NAME
export STACK_NAME := ${USER}
export EC2_KEY := geotrellis-cluster
export AWS_DEFAULT_REGION := us-east-1
export S3_URI := s3://geotrellis-test/ca/${STACK_NAME}
export SUBNET_ID := subnet-c5fefdb1

export MASTER_INSTANCE := m3.xlarge
export MASTER_PRICE := 0.5
export WORKER_INSTANCE := m3.2xlarge
export WORKER_PRICE := 0.5
export WORKER_COUNT := 5

export DRIVER_MEMORY := 4200M
export DRIVER_CORES := 2
export EXECUTOR_MEMORY := 4200M
export EXECUTOR_CORES := 2
export YARN_OVERHEAD := 700

# Docker image of benchmarking service
export SERVICE_IMG := quay.io/geotrellis/comparative-analysis-bastion:${SERVICE_TAG}
export SERVICE_TAG := latest

SPARK_OPTIONS := --master yarn \
--deploy-mode cluster \
--driver-memory ${DRIVER_MEMORY} \
--driver-cores ${DRIVER_CORES} \
--executor-memory ${EXECUTOR_MEMORY} \
--executor-cores ${EXECUTOR_CORES} \
--conf spark.dynamicAllocation.enabled=true \
--conf spark.yarn.executor.memoryOverhead=${YARN_OVERHEAD} \
--conf spark.yarn.driver.memoryOverhead=${YARN_OVERHEAD}


GEODOCKER_BOOTSTRAP=s3://geotrellis-test/geodocker/bootstrap-geodocker-accumulo.sh

EMR_IP=$(shell $(warning "Waiting for emr cluster ${1}") \
  aws emr wait cluster-running --cluster-id ${1} 1>&2 \
	&& aws emr describe-cluster --output json  --cluster-id ${1} | jq -r '.Cluster.MasterPublicDnsName')

define EMR_CREATE_CLUSTER
	$(warning "Creating ${1} cluster")
	aws emr create-cluster --name "CA ${STACK_NAME} ${1}" \
--release-label emr-5.0.0 \
--output text \
--use-default-roles \
--log-uri ${S3_URI}/logs \
--ec2-attributes KeyName=${EC2_KEY},SubnetId=${SUBNET_ID} \
--applications Name=Hadoop Name=Zookeeper Name=Spark Name=Zeppelin \
--instance-groups \
Name=Master,BidPrice=${MASTER_PRICE},InstanceCount=1,InstanceGroupType=MASTER,InstanceType=${MASTER_INSTANCE} \
Name=Workers,BidPrice=${WORKER_PRICE},InstanceCount=${WORKER_COUNT},InstanceGroupType=CORE,InstanceType=${WORKER_INSTANCE} \
--bootstrap-actions Name=bootstrap-${1},Path=${GEODOCKER_BOOTSTRAP},Args=[-i=${2},-n=gis,-p=secret] \
| tee ${STACK_NAME}-${1}-cluster-id.txt
endef

GEOWAVE_CLUSTER_ID=$(shell cat ${STACK_NAME}-geowave-cluster-id.txt)
GEOMESA_CLUSTER_ID=$(shell cat ${STACK_NAME}-geomesa-cluster-id.txt)

GEOWAVE_ZOOKEEPER=$(call EMR_IP,${GEOWAVE_CLUSTER_ID})
GEOMESA_ZOOKEEPER=$(call EMR_IP,${GEOMESA_CLUSTER_ID})

${STACK_NAME}-geomesa-cluster-id.txt:
	$(call EMR_CREATE_CLUSTER,geomesa,quay.io/geodocker/accumulo-geomesa)

${STACK_NAME}-geowave-cluster-id.txt:
	$(call EMR_CREATE_CLUSTER,geowave,quay.io/geodocker/accumulo-geowave)

deploy: ${STACK_NAME}-geomesa-cluster-id.txt ${STACK_NAME}-geowave-cluster-id.txt
	terraform apply \
-state="${STACK_NAME}.tfstate" \
-var 'stack_name=${STACK_NAME}' \
-var 'ec2_key=${EC2_KEY}' \
-var 'subnet_id=${SUBNET_ID}' \
-var 'service_image=${SERVICE_IMG}' \
-var 'geomesa_zookeeper=${GEOMESA_ZOOKEEPER}' \
-var 'geowave_zookeeper=${GEOWAVE_ZOOKEEPER}' \
-var 'geomesa_cluster_id=${GEOMESA_CLUSTER_ID}' \
-var 'geowave_cluster_id=${GEOWAVE_CLUSTER_ID}'

destroy-service:
	terraform destroy -force \
-state="${STACK_NAME}.tfstate" \
-var 'stack_name=${STACK_NAME}' \
-var 'ec2_key=${EC2_KEY}' \
-var 'subnet_id=${SUBNET_ID}' \
-var 'service_image=NA' \
-var 'geomesa_zookeeper=NA' \
-var 'geowave_zookeeper=NA' \
-var 'geomesa_cluster_id=NA' \
-var 'geowave_cluster_id=NA'


destroy: destroy-service
	aws emr terminate-clusters --cluster-ids ${GEOMESA_CLUSTER_ID} ${GEOWAVE_CLUSTER_ID}
	@rm -f *-cluster-id.txt

ingest-synthetic-data-gm: ASSEMBLY_URI=$(shell ./stage-assembly.sh ../synthetic-data mesaPoke ${S3_URI}/jars)
ingest-synthetic-data-gm:
	@if [ -z "${ASSEMBLY_URI}" ]; then echo "Assembly failed" && exit 1; fi
	./add-steps.sh ${GEOMESA_CLUSTER_ID} "Ingest Synthetic" \
spark-submit ${SPARK_OPTIONS} --class com.azavea.geomesa.MesaPoke ${ASSEMBLY_URI} \
gis ${GEOMESA_ZOOKEEPER} root secret geomesa.test \
point,100,uniform:-180:180,uniform:-90:90,fixed:0,100

ingest-synthetic-data-gw: ASSEMBLY_URI=$(shell ./stage-assembly.sh ../synthetic-data wavePoke ${S3_URI}/jars)
ingest-synthetic-data-gw:
	@if [ -z "${ASSEMBLY_URI}" ]; then echo "Assembly failed" && exit 1; fi
	./add-steps.sh ${GEOWAVE_CLUSTER_ID} "Ingest Synthetic" \
spark-submit ${SPARK_OPTIONS} --class com.azavea.geowave.WavePoke ${ASSEMBLY_URI} \
gis ${GEOWAVE_ZOOKEEPER} root secret geowave.test space \
point,100,uniform:-180:180,uniform:-90:90,fixed:0,100

ingest-geolife-gm: ASSEMBLY_URI=$(shell ./stage-assembly.sh ../empirical-data geomesa ${S3_URI}/jars)
ingest-geolife-gm:
	@if [ -z "${ASSEMBLY_URI}" ]; then echo "Assembly failed" && exit 1; fi
	./add-steps.sh ${GEOMESA_CLUSTER_ID} "Ingest CSV" \
spark-submit ${SPARK_OPTIONS} --class com.azavea.ingest.geomesa.Main ${ASSEMBLY_URI} \
csv -i gis -z ${GEOMESA_ZOOKEEPER} -u root \
-p secret -t geomesa.test -e plt -s , -d 6 \
--codec 'the_geom=point($$2,$$1),height=$$4,timestamp=date({yyyy-MM-ddHH:mm:ss},concat($$6,$$7))' \
--featurename gmtrajectory \
geotrellis-sample-datasets geolife/

ingest-geolife-gw: ASSEMBLY_URI=$(shell ./stage-assembly.sh ../empirical-data geowave ${S3_URI}/jars)
ingest-geolife-gw:
	@if [ -z "${ASSEMBLY_URI}" ]; then echo "Assembly failed" && exit 1; fi
	./add-steps.sh ${GEOWAVE_CLUSTER_ID} "Ingest CSV" \
spark-submit ${SPARK_OPTIONS} --class com.azavea.ingest.geowave.Main ${ASSEMBLY_URI} \
csv -i gis -z ${GEOWAVE_ZOOKEEPER} -u root \
-p secret -t geowave.test -e plt -s , -d 6 \
--codec 'the_geom=point($$2,$$1),height=$$4,timestamp=date({yyyy-MM-ddHH:mm:ss},concat($$6,$$7))' \
--featurename gwtrajectory \
geotrellis-sample-datasets geolife/

ingest-shp-data-gm: ASSEMBLY_URI=$(shell ./stage-assembly.sh ../empirical-data geomesa ${S3_URI}/jars)
ingest-shp-data-gm:
	@if [ -z "${ASSEMBLY_URI}" ]; then echo "Assembly failed" && exit 1; fi
	./add-steps.sh ${GEOMESA_CLUSTER_ID} "Ingest shapefiles" \
spark-submit ${SPARK_OPTIONS} --class com.azavea.ingest.geomesa.Main  ${ASSEMBLY_URI} \
shapefile -i gis -z ${GEOMESA_ZOOKEEPER} -u root \
-p secret -t geomesa.shptest \
--featurename gmgenerated-tracks \
geotrellis-sample-datasets generated-tracks/
