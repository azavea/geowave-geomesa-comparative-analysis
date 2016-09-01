# Comparative Analysis Deployment

To run a benchmark requires three components:
    - An Accumulo cluster running GeoWave
    - An Accumulo cluster running GeoMesa
    - A collection of benchmark clients able to query either


## Benchmark Service
The benchmark client is a REST service that is distributed as a Docker image.
In order to scale the load we deploy the benchmark service on ECS backed by an auto-scaling group.
We use Terraform to simplify the deployment of somewhat large number of components that support ECS.

Inputs to ECS service are listed in `variables.tf`.


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
