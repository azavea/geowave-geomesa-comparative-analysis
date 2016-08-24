output "service_elb_ip" {
  value = "${aws_elb.ca.dns_name}"
}
