variable "stack_name" {
  type = "string"
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
  default = "arn:aws:iam::896538046175:instance-profile/terraform-ge5ngo62hrczjc4mbzrs5y2esu"
}
