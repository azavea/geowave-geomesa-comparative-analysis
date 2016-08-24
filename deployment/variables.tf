variable "ecs_cluster_name" {
  default = "Comparative_Analysis"
}

variable "ec2_key" {
  type = "string"
}

variable "subnet_id" {
  type = "string"
}

variable "geomesa_zookeeper"{
  type = "string"
}

variable "geowave_zookeeper"{
  type = "string"
}

variable "desired_benchmark_instance_count" {
  default = 2
}

variable "aws_ecs_ami" {
  default = "ami-52cd5445"
}

variable "ecs_instance_type" {
  default = "m3.large"
}
