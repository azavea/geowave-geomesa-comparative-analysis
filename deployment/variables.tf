variable "stack_name" {
  type = "string"
  description = "Disambiguation prefix for the EMR/ECS stack"
}

variable "service_image" {
  type = "string"
  description = "Benchmark service Docker image"
}

variable "ec2_key" {
  type = "string"
  description = "EC2 key for EMR and ECS machines"
}

variable "subnet_id" {
  type = "string"
  description = "Subnet ID shared by EMR and ECS"
}

variable "geomesa_zookeeper"{
  type = "string"
  description = "Public address of GeoMesa EMR Master"
}

variable "geowave_zookeeper"{
  type = "string"
  description = "Public address of GeoWave EMR Master"
}

variable "desired_benchmark_instance_count" {
  default = 2
  description = "Number benchmark instances to provision"
}

# TODO: make this a dynamic lookup
variable "aws_ecs_ami" {
  default = "ami-6bb2d67c"
}

variable "ecs_instance_type" {
  default = "m3.large"
}

variable "ecs_service_role" {
  default = "arn:aws:iam::896538046175:role/ecs_service_role"
}

variable "ecs_instance_profile" {
  default = "arn:aws:iam::896538046175:instance-profile/terraform-wzxkyowirnachcosiqxrriheki"
}
