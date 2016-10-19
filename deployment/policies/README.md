# ECS IAM Roles

These files contain the sample IAM roles for ECS host and service resources.

Because creating roles requires the highest level of AWS access they are not handled by terraform and are expected to be created manually.
The role must be `ecs_hostl_role` and `ecs_service_role` respectivly.
