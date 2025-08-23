# Cloud-Native Infrastructure Architecture

## Infrastructure Overview

### Multi-Cloud Strategy
- **Primary Cloud**: AWS (East Coast - us-east-1)
- **Secondary Cloud**: Google Cloud Platform (West Coast - us-west1)
- **Edge Computing**: Cloudflare Workers + CDN
- **Disaster Recovery**: Multi-region active-passive setup

### Infrastructure Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Global Infrastructure                    │
│                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ Cloudflare  │  │   Route53   │  │   Global Load       │  │
│  │ Edge/CDN    │  │    DNS      │  │   Balancer          │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────┬───────────────────────────────────────┘
                      │
    ┌─────────────────┼─────────────────┐
    ▼                                   ▼
┌──────────────────────────┐  ┌──────────────────────────┐
│     Primary Region       │  │    Secondary Region      │
│      (us-east-1)         │  │      (us-west-2)         │
│                          │  │                          │
│  ┌─────────────────────┐ │  │  ┌─────────────────────┐ │
│  │   EKS Cluster       │ │  │  │   GKE Cluster       │ │
│  │  (Production)       │ │  │  │ (DR/Development)    │ │
│  └─────────────────────┘ │  │  └─────────────────────┘ │
│  ┌─────────────────────┐ │  │  ┌─────────────────────┐ │
│  │   RDS Aurora        │ │  │  │   Cloud SQL         │ │
│  │  (Multi-AZ)         │ │  │  │   (HA Setup)        │ │
│  └─────────────────────┘ │  │  └─────────────────────┘ │
│  ┌─────────────────────┐ │  │  ┌─────────────────────┐ │
│  │   ElastiCache       │ │  │  │   Memorystore       │ │
│  │   Redis Cluster     │ │  │  │   Redis             │ │
│  └─────────────────────┘ │  │  └─────────────────────┘ │
└──────────────────────────┘  └──────────────────────────┘
```

## Kubernetes Architecture

### EKS Cluster Configuration

```yaml
# EKS Cluster specification
apiVersion: eksctl.io/v1alpha5
kind: ClusterConfig

metadata:
  name: lp-production
  region: us-east-1
  version: "1.28"

# VPC Configuration
vpc:
  cidr: "10.0.0.0/16"
  nat:
    gateway: HighlyAvailable
  clusterEndpoints:
    privateAccess: true
    publicAccess: true
    publicAccessCIDRs: ["0.0.0.0/0"]

# IAM Configuration
iam:
  serviceRolePermissionsBoundary: "arn:aws:iam::ACCOUNT:policy/EKSServiceRolePermissionsBoundary"
  withOIDC: true

# Managed Node Groups
managedNodeGroups:
  - name: system-nodes
    instanceType: t3.large
    minSize: 3
    maxSize: 10
    desiredCapacity: 3
    volumeSize: 100
    volumeType: gp3
    labels:
      role: system
    taints:
      - key: "system"
        value: "true"
        effect: "NoSchedule"

  - name: application-nodes
    instanceTypes: 
      - c5.xlarge
      - c5.2xlarge
    minSize: 5
    maxSize: 50
    desiredCapacity: 10
    volumeSize: 200
    volumeType: gp3
    spot: true
    labels:
      role: application
    
  - name: memory-optimized
    instanceType: r5.xlarge
    minSize: 2
    maxSize: 20
    desiredCapacity: 3
    volumeSize: 100
    volumeType: gp3
    labels:
      role: cache
      workload: redis

# Add-ons
addons:
  - name: vpc-cni
    version: latest
  - name: coredns
    version: latest
  - name: kube-proxy
    version: latest
  - name: aws-ebs-csi-driver
    version: latest

# CloudWatch Logging
cloudWatch:
  clusterLogging:
    enableTypes: ["*"]
```

### Service Mesh - Istio Configuration

```yaml
# Istio Gateway
apiVersion: networking.istio.io/v1alpha3
kind: Gateway
metadata:
  name: lp-gateway
  namespace: istio-system
spec:
  selector:
    istio: ingressgateway
  servers:
  - port:
      number: 443
      name: https
      protocol: HTTPS
    tls:
      mode: SIMPLE
      credentialName: lp-tls-secret
    hosts:
    - api.lp-system.com
    - app.lp-system.com

# Virtual Service for API routing
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: api-routing
spec:
  hosts:
  - api.lp-system.com
  gateways:
  - lp-gateway
  http:
  - match:
    - uri:
        prefix: "/graphql"
    route:
    - destination:
        host: graphql-gateway.production.svc.cluster.local
        port:
          number: 4000
    timeout: 30s
    retries:
      attempts: 3
      perTryTimeout: 10s
  
  - match:
    - uri:
        prefix: "/api/v2"
    route:
    - destination:
        host: api-gateway.production.svc.cluster.local
        port:
          number: 3000
    timeout: 30s

# Destination Rule for load balancing
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: api-gateway-dr
spec:
  host: api-gateway.production.svc.cluster.local
  trafficPolicy:
    loadBalancer:
      simple: LEAST_CONN
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        http1MaxPendingRequests: 50
        http2MaxRequests: 100
        maxRequestsPerConnection: 2
    circuitBreaker:
      consecutiveErrors: 5
      interval: 10s
      baseEjectionTime: 30s
      maxEjectionPercent: 50
```

## Container Orchestration

### Application Deployment Manifests

```yaml
# API Gateway Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-gateway
  namespace: production
  labels:
    app: api-gateway
    version: v2.0.0
spec:
  replicas: 5
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1
      maxSurge: 2
  selector:
    matchLabels:
      app: api-gateway
  template:
    metadata:
      labels:
        app: api-gateway
        version: v2.0.0
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "3001"
        prometheus.io/path: "/metrics"
    spec:
      serviceAccountName: api-gateway-sa
      containers:
      - name: api-gateway
        image: lp-system/api-gateway:v2.0.0
        ports:
        - containerPort: 3000
          name: http
        - containerPort: 3001
          name: metrics
        env:
        - name: NODE_ENV
          value: "production"
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: database-secret
              key: connection-string
        - name: REDIS_URL
          valueFrom:
            secretKeyRef:
              name: redis-secret
              key: connection-string
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /health
            port: 3000
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /ready
            port: 3000
          initialDelaySeconds: 5
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 2

---
# Horizontal Pod Autoscaler
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: api-gateway-hpa
  namespace: production
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: api-gateway
  minReplicas: 5
  maxReplicas: 50
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  - type: Pods
    pods:
      metric:
        name: requests_per_second
      target:
        type: AverageValue
        averageValue: "100"
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Percent
        value: 100
        periodSeconds: 60
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 10
        periodSeconds: 60

---
# Service
apiVersion: v1
kind: Service
metadata:
  name: api-gateway
  namespace: production
  labels:
    app: api-gateway
spec:
  selector:
    app: api-gateway
  ports:
  - name: http
    port: 3000
    targetPort: 3000
    protocol: TCP
  type: ClusterIP
```

### Database Infrastructure

```yaml
# PostgreSQL Aurora Configuration (Terraform)
resource "aws_rds_cluster" "postgresql" {
  cluster_identifier      = "lp-postgresql-cluster"
  engine                 = "aurora-postgresql"
  engine_version         = "15.4"
  database_name          = "lp_production"
  master_username        = "lp_admin"
  master_password        = random_password.db_password.result
  
  backup_retention_period = 35
  preferred_backup_window = "03:00-04:00"
  preferred_maintenance_window = "sun:04:00-sun:05:00"
  
  vpc_security_group_ids = [aws_security_group.database.id]
  db_subnet_group_name   = aws_db_subnet_group.database.name
  
  # Performance and monitoring
  performance_insights_enabled = true
  performance_insights_retention_period = 7
  monitoring_interval = 60
  monitoring_role_arn = aws_iam_role.rds_monitoring.arn
  
  # Security
  storage_encrypted = true
  kms_key_id       = aws_kms_key.database.arn
  
  # Scaling
  serverlessv2_scaling_configuration {
    max_capacity = 64
    min_capacity = 0.5
  }
  
  deletion_protection = true
  skip_final_snapshot = false
  final_snapshot_identifier = "${local.cluster_name}-final-snapshot-${formatdate("YYYY-MM-DD-hhmm", timestamp())}"
  
  tags = local.common_tags
}

# Aurora cluster instances
resource "aws_rds_cluster_instance" "cluster_instances" {
  count              = 3
  identifier         = "lp-aurora-${count.index + 1}"
  cluster_identifier = aws_rds_cluster.postgresql.id
  instance_class     = "db.r6g.xlarge"
  engine             = aws_rds_cluster.postgresql.engine
  engine_version     = aws_rds_cluster.postgresql.engine_version
  
  performance_insights_enabled = true
  monitoring_role_arn          = aws_iam_role.rds_monitoring.arn
  monitoring_interval          = 60
  
  tags = local.common_tags
}

# Read replica in different region
resource "aws_rds_cluster" "postgresql_replica" {
  provider = aws.us_west_2
  
  cluster_identifier              = "lp-postgresql-replica"
  replication_source_identifier   = aws_rds_cluster.postgresql.arn
  engine                         = "aurora-postgresql"
  
  vpc_security_group_ids = [aws_security_group.database_replica.id]
  db_subnet_group_name   = aws_db_subnet_group.database_replica.name
  
  storage_encrypted = true
  kms_key_id       = aws_kms_key.database_replica.arn
  
  tags = local.common_tags
}
```

### Redis Cluster Configuration

```yaml
# Redis ElastiCache Configuration
resource "aws_elasticache_replication_group" "redis" {
  replication_group_id         = "lp-redis-cluster"
  description                  = "Redis cluster for LP production"
  
  node_type                   = "cache.r6g.xlarge"
  port                        = 6379
  parameter_group_name        = aws_elasticache_parameter_group.redis.name
  
  num_cache_clusters          = 6
  automatic_failover_enabled  = true
  multi_az_enabled           = true
  
  subnet_group_name          = aws_elasticache_subnet_group.redis.name
  security_group_ids         = [aws_security_group.redis.id]
  
  # Backup configuration
  snapshot_retention_limit   = 7
  snapshot_window           = "03:00-05:00"
  maintenance_window        = "sun:05:00-sun:07:00"
  
  # Security
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  auth_token                = random_password.redis_auth.result
  
  # Logging
  log_delivery_configuration {
    destination      = aws_cloudwatch_log_group.redis_slow.name
    destination_type = "cloudwatch-logs"
    log_format      = "text"
    log_type        = "slow-log"
  }
  
  tags = local.common_tags
}

# Redis parameter group
resource "aws_elasticache_parameter_group" "redis" {
  family = "redis7"
  name   = "lp-redis-params"

  # Performance optimizations
  parameter {
    name  = "maxmemory-policy"
    value = "allkeys-lru"
  }
  
  parameter {
    name  = "maxmemory-samples"
    value = "10"
  }
  
  parameter {
    name  = "timeout"
    value = "300"
  }
  
  parameter {
    name  = "tcp-keepalive"
    value = "300"
  }
}
```

## File Storage & CDN

### AWS S3 + CloudFront Configuration

```yaml
# S3 Bucket for file storage
resource "aws_s3_bucket" "assets" {
  bucket = "lp-system-assets-${random_id.bucket_suffix.hex}"
  
  tags = local.common_tags
}

# S3 bucket versioning
resource "aws_s3_bucket_versioning" "assets" {
  bucket = aws_s3_bucket.assets.id
  versioning_configuration {
    status = "Enabled"
  }
}

# S3 bucket encryption
resource "aws_s3_bucket_server_side_encryption_configuration" "assets" {
  bucket = aws_s3_bucket.assets.id

  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = aws_kms_key.s3.arn
      sse_algorithm     = "aws:kms"
    }
    bucket_key_enabled = true
  }
}

# S3 bucket lifecycle
resource "aws_s3_bucket_lifecycle_configuration" "assets" {
  bucket = aws_s3_bucket.assets.id

  rule {
    id     = "asset_lifecycle"
    status = "Enabled"

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    transition {
      days          = 90
      storage_class = "GLACIER"
    }

    transition {
      days          = 365
      storage_class = "DEEP_ARCHIVE"
    }

    noncurrent_version_expiration {
      noncurrent_days = 90
    }
  }
}

# CloudFront distribution
resource "aws_cloudfront_distribution" "assets" {
  origin {
    domain_name = aws_s3_bucket.assets.bucket_regional_domain_name
    origin_id   = "S3-${aws_s3_bucket.assets.id}"
    
    s3_origin_config {
      origin_access_identity = aws_cloudfront_origin_access_identity.assets.cloudfront_access_identity_path
    }
  }

  enabled             = true
  is_ipv6_enabled     = true
  default_root_object = "index.html"

  # Cache behaviors
  default_cache_behavior {
    allowed_methods        = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods         = ["GET", "HEAD"]
    target_origin_id       = "S3-${aws_s3_bucket.assets.id}"
    compress               = true
    viewer_protocol_policy = "redirect-to-https"

    forwarded_values {
      query_string = false
      headers      = ["Origin", "Access-Control-Request-Headers", "Access-Control-Request-Method"]
      
      cookies {
        forward = "none"
      }
    }

    min_ttl     = 0
    default_ttl = 86400
    max_ttl     = 31536000
  }

  # Images cache behavior
  ordered_cache_behavior {
    path_pattern     = "/images/*"
    allowed_methods  = ["GET", "HEAD", "OPTIONS"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "S3-${aws_s3_bucket.assets.id}"
    compress         = true

    forwarded_values {
      query_string = true
      headers      = ["CloudFront-Is-*-Viewer"]
      cookies {
        forward = "none"
      }
    }

    viewer_protocol_policy = "redirect-to-https"
    min_ttl               = 0
    default_ttl           = 2592000  # 30 days
    max_ttl               = 31536000 # 1 year
  }

  # Geographic restrictions
  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  # SSL certificate
  viewer_certificate {
    acm_certificate_arn      = aws_acm_certificate.assets_cert.arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }

  # WAF integration
  web_acl_id = aws_wafv2_web_acl.assets.arn

  tags = local.common_tags
}
```

## Monitoring & Observability

### Prometheus + Grafana Stack

```yaml
# Prometheus Operator
apiVersion: v1
kind: Namespace
metadata:
  name: monitoring

---
# Prometheus configuration
apiVersion: monitoring.coreos.com/v1
kind: Prometheus
metadata:
  name: prometheus
  namespace: monitoring
spec:
  replicas: 2
  retention: 30d
  storage:
    volumeClaimTemplate:
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 100Gi
        storageClassName: gp3
  
  serviceMonitorSelector:
    matchLabels:
      team: lp-system
  
  ruleSelector:
    matchLabels:
      team: lp-system
  
  resources:
    requests:
      memory: 4Gi
      cpu: 2
    limits:
      memory: 8Gi
      cpu: 4

  # High availability
  shards: 2
  replicaExternalLabelName: "replica"
  prometheusExternalLabelName: "prometheus"

---
# Grafana deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: grafana
  namespace: monitoring
spec:
  replicas: 2
  selector:
    matchLabels:
      app: grafana
  template:
    metadata:
      labels:
        app: grafana
    spec:
      containers:
      - name: grafana
        image: grafana/grafana:10.2.0
        ports:
        - containerPort: 3000
        env:
        - name: GF_SECURITY_ADMIN_PASSWORD
          valueFrom:
            secretKeyRef:
              name: grafana-secret
              key: admin-password
        - name: GF_DATABASE_TYPE
          value: postgres
        - name: GF_DATABASE_HOST
          value: postgres.monitoring.svc.cluster.local:5432
        volumeMounts:
        - name: grafana-storage
          mountPath: /var/lib/grafana
        - name: grafana-config
          mountPath: /etc/grafana/provisioning
        resources:
          requests:
            memory: 512Mi
            cpu: 250m
          limits:
            memory: 1Gi
            cpu: 500m
      volumes:
      - name: grafana-storage
        persistentVolumeClaim:
          claimName: grafana-pvc
      - name: grafana-config
        configMap:
          name: grafana-config
```

This infrastructure design ensures 99.99% uptime with automatic scaling, disaster recovery, and comprehensive monitoring capabilities.