# Comparative Analysis Deployment

Though equivalence of development and deployment environments is ideal, it is often difficult to achieve in practice. While a local cluster with GeoDocker can currently be brought online with a single command, we opted to use the YARN, Zookeeper, and HDFS which is distributed on Amazon EMR to support GeoDocker’s Accumulo processes. This was done primarily out of expedience: both projects currently document an EMR-based deployment and the administration of these EMR managed technologies is complex enough to warrant some slippage between development and deployment if the differences mitigate the complexity of running long-lived clusters.
	Pictured below is a rough diagram of the deployment most consistently used throughout our efforts. The entire infrastructure for actually running queries and collecting timings runs on AWS and orchestrated through a combination of makefiles (a simple tool for organizing common tasks) and Hashicorp’s Terraform which provides the means for specifying, in an idempotent fashion, a set of AWS resources. The machines and the software running on top of them were not especially tuned for performance. Instead, we opted to use default settings to see how each system operates under plausible (though likely non-optimal) circumstances.

![Test environment architecture](../docs/img/test-environment-architecture.png)

To bring this system online, requests are sent over the AWS command line client to bring up EMR clusters of whatever size is required (typically 3 or 5 workers) for each of GeoMesa and GeoWave. Once the bare EMR cluster is online, Docker images for GeoServer, Accumulo and the GeoWave or GeoMesa iterators are pulled down and run. Upon loading both GeoWave and GeoMesa their generated cluster IDs are used to register them with a cluster of ECS query servers (each of which is identical and stateless to allow for simple scaling). These ECS-backed query servers all sit behind an AWS load balancer to ensure adequate throughput so that the likelihood of testing artifacts due to network problems is reduced.
Once the system is online with data ingested (see the next section for details on our handling of this task), there’s still the question of persisting the results of queries. To simplify and centralize the process, we opted to use an AWS DynamoDB table which the query servers write timings and other important data to at the end of their response cycles. By keeping all timings in Amazon’s cloud, results are further insulated from network-related artifacts.

## Details

To run a benchmark requires three components:
    - An Accumulo cluster running GeoWave
    - An Accumulo cluster running GeoMesa
    - A collection of benchmark clients able to query either


## Benchmark Service
The benchmark client is a REST service that is distributed as a Docker image.
In order to scale the load we deploy the benchmark service on ECS backed by an auto-scaling group.
We use Terraform to simplify the deployment of somewhat large number of components that support ECS.

Inputs to ECS service are listed in `variables.tf`.

You can quickly update the container used by ECS for a given set of GeoWave and GeoMesa clusters by altering the tag (or, indeed, the image name) of the query server’s container in the deployment Makefile, modified query server code is pulled down and run by the machines on Amazon ECS.

## Accumulo Clusters
Both the GeoWave and GeoMesa clusters are started on EMR, using GeoDocker images.

## Deployment

*Requires:* terraform cli 0.7

Unfortunately terraform does not support provisioning EMR clusters as of version 0.7.1.
This requires us to use aws cli to create the EMR cluster and wait for them start.
At that point we can query the aws emr API using the cli and fetch the cluster IP.

*Note:* since we have to wait for the EMR machines to be provisioned before we know the cluster IP there is an unusually long wait in the deploy process where the scripts wait for this event.

This information is passed to terraform as `var` arguments and eventually passed to the benchmark service container as environment variables expected by its `application.conf`.
Since the ECS cluster and EMR cluster are on the same VPC and Subnet we can expect the benchmark containers to be able to reach Accumulo on EMR cluster.

Because this two-stage deployment is more complicated we use a `Makefile` to manage the deployment process:

```sh
# To deploy
> make deploy

# To destroy
> make destroy
```

### IAM Roles and Permissions

The IAM role management is not handled by terraform. These IAM roles must be included in.
Required polices are included in the `policies` folder.

Additionally the EMR default roles must allow access to both workers and master from the `default` security group.
ECS instances created by the terraform script are placed in the `default` security group because none are specified.

### Multiple Stacks

You can deploy multiple stacks by providing a different stack name, which will be used in the file name for the terraform state and the EMR cluster IDs.
The default value for `${STACK_NAME}` is your username.

```sh
# To deploy
> make deploy STACK_NAME=purple

# To destroy
> make destroy STACK_NAME=purple
```


The `Makefile` will save the cluster ids in `${STACK_NAME}-geowave-cluster-id.txt` and `${STACK_NAME}-geomesa-cluster-id.txt`
