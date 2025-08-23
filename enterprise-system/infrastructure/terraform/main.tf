terraform {
  required_version = ">= 1.5.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.11"
    }
  }
  
  backend "s3" {
    bucket = "enterprise-terraform-state"
    key    = "infrastructure/terraform.tfstate"
    region = "us-east-1"
    encrypt = true
    dynamodb_table = "terraform-state-lock"
  }
}

# Provider Configuration
provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Environment = var.environment
      Project     = "enterprise-system"
      ManagedBy   = "terraform"
    }
  }
}

# Data Sources
data "aws_availability_zones" "available" {
  state = "available"
}

# VPC Module
module "vpc" {
  source = "./modules/networking"
  
  vpc_cidr             = var.vpc_cidr
  environment          = var.environment
  availability_zones   = data.aws_availability_zones.available.names
  private_subnet_cidrs = var.private_subnet_cidrs
  public_subnet_cidrs  = var.public_subnet_cidrs
}

# EKS Cluster
module "eks" {
  source = "./modules/compute"
  
  cluster_name    = "${var.project_name}-${var.environment}"
  cluster_version = var.kubernetes_version
  vpc_id          = module.vpc.vpc_id
  subnet_ids      = module.vpc.private_subnet_ids
  
  node_groups = {
    general = {
      desired_size = 3
      min_size     = 2
      max_size     = 10
      
      instance_types = ["t3.large"]
      
      labels = {
        role = "general"
      }
    }
    
    spot = {
      desired_size = 2
      min_size     = 1
      max_size     = 5
      
      instance_types = ["t3.large", "t3a.large"]
      capacity_type  = "SPOT"
      
      labels = {
        role = "spot"
        workload = "batch"
      }
      
      taints = [{
        key    = "spot"
        value  = "true"
        effect = "NO_SCHEDULE"
      }]
    }
  }
}

# RDS PostgreSQL
module "rds" {
  source = "./modules/database"
  
  identifier     = "${var.project_name}-${var.environment}"
  engine_version = "15.4"
  instance_class = var.rds_instance_class
  
  allocated_storage     = 100
  max_allocated_storage = 1000
  
  database_name = "enterprise"
  username      = "dbadmin"
  
  vpc_id                  = module.vpc.vpc_id
  subnet_ids              = module.vpc.database_subnet_ids
  allowed_security_groups = [module.eks.cluster_security_group_id]
  
  backup_retention_period = 30
  backup_window          = "03:00-04:00"
  maintenance_window     = "sun:04:00-sun:05:00"
  
  multi_az               = var.environment == "production"
  deletion_protection    = var.environment == "production"
  
  enabled_cloudwatch_logs_exports = ["postgresql"]
}

# ElastiCache Redis
module "redis" {
  source = "./modules/cache"
  
  cluster_id      = "${var.project_name}-${var.environment}"
  node_type       = var.redis_node_type
  num_cache_nodes = var.environment == "production" ? 3 : 1
  
  engine_version = "7.0"
  port           = 6379
  
  vpc_id                  = module.vpc.vpc_id
  subnet_ids              = module.vpc.private_subnet_ids
  allowed_security_groups = [module.eks.cluster_security_group_id]
  
  automatic_failover_enabled = var.environment == "production"
  multi_az_enabled          = var.environment == "production"
  
  snapshot_retention_limit = var.environment == "production" ? 7 : 1
  snapshot_window         = "03:00-05:00"
}

# MSK Kafka Cluster
module "kafka" {
  source = "./modules/streaming"
  
  cluster_name = "${var.project_name}-${var.environment}"
  
  kafka_version = "3.5.1"
  
  number_of_broker_nodes = var.environment == "production" ? 3 : 2
  instance_type         = var.kafka_instance_type
  
  ebs_volume_size = 100
  
  vpc_id                  = module.vpc.vpc_id
  subnet_ids              = module.vpc.private_subnet_ids
  allowed_security_groups = [module.eks.cluster_security_group_id]
  
  enable_monitoring = true
  
  configuration_properties = {
    "auto.create.topics.enable"  = "true"
    "delete.topic.enable"        = "true"
    "log.retention.hours"        = "168"
    "num.partitions"             = "3"
    "default.replication.factor" = var.environment == "production" ? "3" : "2"
  }
}

# S3 Buckets
module "s3" {
  source = "./modules/storage"
  
  buckets = {
    "${var.project_name}-${var.environment}-data" = {
      versioning = true
      lifecycle_rules = [{
        id      = "archive"
        enabled = true
        
        transition = [{
          days          = 30
          storage_class = "STANDARD_IA"
        }, {
          days          = 90
          storage_class = "GLACIER"
        }]
        
        expiration = {
          days = 365
        }
      }]
    }
    
    "${var.project_name}-${var.environment}-backups" = {
      versioning = true
      lifecycle_rules = [{
        id      = "delete-old-backups"
        enabled = true
        
        expiration = {
          days = 90
        }
      }]
    }
  }
}

# Route53 DNS
module "dns" {
  source = "./modules/dns"
  
  domain_name = var.domain_name
  
  records = {
    "api" = {
      type    = "A"
      alias   = true
      target  = module.alb.dns_name
      zone_id = module.alb.zone_id
    }
  }
}

# Application Load Balancer
module "alb" {
  source = "./modules/load-balancer"
  
  name = "${var.project_name}-${var.environment}"
  
  vpc_id         = module.vpc.vpc_id
  subnet_ids     = module.vpc.public_subnet_ids
  
  certificate_arn = var.certificate_arn
  
  enable_deletion_protection = var.environment == "production"
  enable_http2              = true
  
  access_logs = {
    bucket  = module.s3.bucket_names["${var.project_name}-${var.environment}-logs"]
    enabled = true
  }
}

# Monitoring and Alerting
module "monitoring" {
  source = "./modules/monitoring"
  
  cluster_name = module.eks.cluster_name
  
  sns_topic_email = var.alert_email
  
  alarm_configurations = {
    high_cpu = {
      metric_name         = "CPUUtilization"
      namespace           = "AWS/EKS"
      statistic           = "Average"
      period              = 300
      evaluation_periods  = 2
      threshold           = 80
      comparison_operator = "GreaterThanThreshold"
    }
    
    high_memory = {
      metric_name         = "MemoryUtilization"
      namespace           = "AWS/EKS"
      statistic           = "Average"
      period              = 300
      evaluation_periods  = 2
      threshold           = 85
      comparison_operator = "GreaterThanThreshold"
    }
  }
}