# EC2 instance configuration for running the Spring Boot backend API
# Uses t3.micro (AWS Free Tier eligible) with Ubuntu 24.04

# Security group allowing HTTP (8080) and SSH (22) access
resource "aws_security_group" "backend_sg" {
  name        = "meal-planner-backend-sg"
  description = "Security group for Meal Planner backend API server"
  vpc_id      = aws_vpc.main.id

  # Allow SSH access for deployment
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "SSH access for deployment"
  }

  # Allow HTTP access on port 8080 for Spring Boot API
  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Spring Boot API access"
  }

  # Allow all outbound traffic for package installation and DB access
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow all outbound traffic"
  }

  tags = {
    Name = "meal-planner-backend-sg"
  }
}

# EC2 instance running the Spring Boot backend application
resource "aws_instance" "backend" {
  ami                    = var.ami_id
  instance_type          = var.instance_type
  subnet_id              = aws_subnet.public_a.id
  vpc_security_group_ids = [aws_security_group.backend_sg.id]
  key_name               = var.key_pair_name

  # User data script installs Java 17 and configures systemd service (Ubuntu 24.04)
  user_data = <<-EOF
              #!/bin/bash
              apt-get update -y
              apt-get install -y openjdk-17-jdk
              mkdir -p /opt/mealplanner
              cat > /etc/systemd/system/mealplanner.service <<'SERVICE'
              [Unit]
              Description=Smart Recipe Meal Planner API
              After=network.target
              [Service]
              Type=simple
              User=ubuntu
              ExecStart=/usr/bin/java -jar /opt/mealplanner/meal-planner-api-1.0.0.jar --spring.profiles.active=prod
              Restart=always
              RestartSec=10
              [Install]
              WantedBy=multi-user.target
              SERVICE
              systemctl daemon-reload
              systemctl enable mealplanner
              EOF

  tags = {
    Name    = "meal-planner-backend"
    Project = "SmartRecipeMealPlanner"
  }
}

# Elastic IP for a stable public address (survives instance stop/start)
resource "aws_eip" "backend" {
  instance = aws_instance.backend.id
  domain   = "vpc"

  tags = {
    Name    = "meal-planner-eip"
    Project = "SmartRecipeMealPlanner"
  }
}
