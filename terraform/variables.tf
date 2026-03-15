# Input variables for Terraform infrastructure configuration
# Override these values in terraform.tfvars or via CLI arguments

variable "aws_region" {
  description = "AWS region for deploying all resources"
  type        = string
  default     = "eu-west-1"
}

variable "instance_type" {
  description = "EC2 instance type for the backend server"
  type        = string
  default     = "t3.micro"
}

variable "ami_id" {
  description = "Amazon Machine Image ID for EC2 (Amazon Linux 2023)"
  type        = string
  default     = "ami-0c38b837cd80f13bb"
}

variable "key_pair_name" {
  description = "Name of the EC2 key pair for SSH access"
  type        = string
  default     = "meal-planner-key"
}

variable "db_instance_class" {
  description = "RDS instance class for the PostgreSQL database"
  type        = string
  default     = "db.t3.micro"
}

variable "db_username" {
  description = "Master username for the RDS PostgreSQL instance"
  type        = string
  default     = "postgres"
  sensitive   = true
}

variable "db_password" {
  description = "Master password for the RDS PostgreSQL instance"
  type        = string
  sensitive   = true
}

variable "s3_bucket_name" {
  description = "Globally unique S3 bucket name for frontend static hosting"
  type        = string
  default     = "smart-meal-planner-25160052"
}
