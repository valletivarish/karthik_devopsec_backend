# RDS PostgreSQL configuration for the meal planner database
# Uses db.t3.micro (AWS Free Tier eligible) in private subnets

# DB subnet group spanning two availability zones for high availability
resource "aws_db_subnet_group" "main" {
  name       = "meal-planner-db-subnet"
  subnet_ids = [aws_subnet.private_a.id, aws_subnet.private_b.id]

  tags = {
    Name = "meal-planner-db-subnet-group"
  }
}

# Security group for RDS allowing PostgreSQL access from backend EC2 only
resource "aws_security_group" "rds_sg" {
  name        = "meal-planner-rds-sg"
  description = "Security group for Meal Planner RDS PostgreSQL"
  vpc_id      = aws_vpc.main.id

  # Allow PostgreSQL connections only from the backend security group
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.backend_sg.id]
    description     = "PostgreSQL access from backend"
  }

  tags = {
    Name = "meal-planner-rds-sg"
  }
}

# RDS PostgreSQL instance for production database
resource "aws_db_instance" "main" {
  identifier             = "meal-planner-db"
  engine                 = "postgres"
  engine_version         = "15.10"
  instance_class         = var.db_instance_class
  allocated_storage      = 20
  storage_type           = "gp3"
  db_name                = "mealplanner"
  username               = var.db_username
  password               = var.db_password
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds_sg.id]
  publicly_accessible    = false
  skip_final_snapshot    = true
  multi_az               = false

  tags = {
    Name    = "meal-planner-rds"
    Project = "SmartRecipeMealPlanner"
  }
}
