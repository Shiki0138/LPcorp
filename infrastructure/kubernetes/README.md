# Enterprise System Kubernetes Infrastructure

This directory contains comprehensive Kubernetes manifests for the Enterprise System microservices platform.

## Directory Structure

```
infrastructure/kubernetes/
├── namespaces/                    # Namespace configurations and resource quotas
├── deployments/                   # Service deployment manifests
├── services/                      # Service definitions and service accounts
├── configmaps/                   # Configuration data for services
├── secrets/                      # Secret templates (use sealed-secrets in production)
├── hpa/                         # Horizontal Pod Autoscaler configurations
├── ingress/                     # Kong Ingress Controller and routing
├── kafka/                       # Kafka cluster configuration (Strimzi)
├── postgresql/                  # PostgreSQL StatefulSet with HA setup
├── redis/                       # Redis Master-Slave with Sentinel
├── monitoring/                  # Prometheus and Grafana configurations
├── network-policies/            # Network security policies
├── rbac/                        # Role-Based Access Control
├── storage/                     # Storage classes and persistent volumes
├── helm/enterprise-system/      # Helm chart for the complete system
└── argocd/                      # GitOps configurations with ArgoCD
```

## Quick Start

### Prerequisites

1. **Kubernetes Cluster** (v1.28+)
   - EKS cluster with proper node groups
   - VPC with private/public subnets
   - IAM roles and policies configured

2. **Required Operators/Controllers**:
   ```bash
   # Install required operators
   kubectl apply -f https://github.com/strimzi/strimzi-kafka-operator/releases/download/0.37.0/strimzi-cluster-operator-0.37.0.yaml
   kubectl apply -f https://github.com/prometheus-operator/prometheus-operator/releases/download/v0.67.1/prometheus-operator-crd.yaml
   ```

3. **Storage Classes**:
   ```bash
   kubectl apply -f storage/storage-classes.yaml
   ```

### Deployment Order

1. **Setup Namespaces and RBAC**:
   ```bash
   kubectl apply -f namespaces/
   kubectl apply -f rbac/
   ```

2. **Deploy Infrastructure Components**:
   ```bash
   # PostgreSQL
   kubectl apply -f postgresql/
   
   # Redis
   kubectl apply -f redis/
   
   # Kafka
   kubectl apply -f kafka/
   ```

3. **Deploy Monitoring Stack**:
   ```bash
   kubectl apply -f monitoring/
   ```

4. **Deploy API Gateway**:
   ```bash
   kubectl apply -f ingress/
   ```

5. **Deploy Microservices**:
   ```bash
   # Create secrets first (replace with actual values)
   kubectl apply -f secrets/
   
   # Deploy ConfigMaps
   kubectl apply -f configmaps/
   
   # Deploy services
   kubectl apply -f deployments/
   kubectl apply -f services/
   
   # Apply autoscaling
   kubectl apply -f hpa/
   ```

6. **Apply Network Policies**:
   ```bash
   kubectl apply -f network-policies/
   ```

### Using Helm Chart

For production deployment, use the Helm chart:

```bash
# Development
helm install enterprise-system helm/enterprise-system \
  --namespace development \
  --values helm/enterprise-system/values-dev.yaml

# Staging
helm install enterprise-system helm/enterprise-system \
  --namespace staging \
  --values helm/enterprise-system/values-staging.yaml

# Production
helm install enterprise-system helm/enterprise-system \
  --namespace production \
  --values helm/enterprise-system/values.yaml
```

### GitOps with ArgoCD

1. **Install ArgoCD**:
   ```bash
   kubectl create namespace argocd
   kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
   ```

2. **Deploy Applications**:
   ```bash
   kubectl apply -f argocd/
   ```

## Configuration

### Environment-Specific Values

- **Development**: `helm/enterprise-system/values-dev.yaml`
  - Single replicas
  - Minimal resources
  - Network policies disabled
  - No PCI compliance

- **Staging**: `helm/enterprise-system/values-staging.yaml`
  - Medium replicas
  - Production-like setup
  - Full monitoring
  - PCI compliance enabled

- **Production**: `helm/enterprise-system/values.yaml`
  - High availability
  - Maximum security
  - Full resource allocation
  - Complete monitoring

### Security Features

1. **Pod Security Standards**: Restricted policy for all workloads
2. **Network Policies**: Default deny with explicit allow rules
3. **RBAC**: Least privilege access control
4. **Secrets Management**: Sealed Secrets for GitOps
5. **Service Mesh**: Istio for mTLS and traffic management
6. **Image Security**: Only signed images from trusted registries

### Resource Management

- **Resource Quotas**: Per-namespace limits
- **Limit Ranges**: Default and maximum resource constraints
- **HPA**: Automatic scaling based on CPU/memory/custom metrics
- **VPA**: Vertical scaling recommendations
- **Node Affinity**: Workload-specific node placement

### Monitoring & Observability

- **Metrics**: Prometheus with custom service metrics
- **Dashboards**: Grafana with pre-built dashboards
- **Logging**: Structured logging to stdout
- **Tracing**: OpenTelemetry with Jaeger/Tempo
- **Alerting**: AlertManager with webhook notifications

### High Availability

- **Multi-AZ Deployment**: Pods spread across availability zones
- **Database Replication**: PostgreSQL primary-replica setup
- **Cache Redundancy**: Redis Sentinel for automatic failover
- **Message Queuing**: Kafka cluster with replication
- **Load Balancing**: Kong with multiple replicas

## Troubleshooting

### Common Issues

1. **Pod Stuck in Pending**:
   ```bash
   kubectl describe pod <pod-name> -n <namespace>
   # Check node resources and taints
   ```

2. **Service Discovery Issues**:
   ```bash
   kubectl get endpoints -n <namespace>
   kubectl describe service <service-name> -n <namespace>
   ```

3. **Network Policy Blocking Traffic**:
   ```bash
   kubectl describe networkpolicy -n <namespace>
   # Temporarily disable network policies for debugging
   ```

4. **Database Connection Issues**:
   ```bash
   kubectl exec -it postgres-primary-0 -n production -- psql -U postgres
   kubectl logs -f postgres-primary-0 -n production
   ```

### Monitoring Commands

```bash
# Check cluster resources
kubectl top nodes
kubectl top pods --all-namespaces

# Monitor deployments
kubectl get deployments -n production -w

# Check HPA status
kubectl get hpa -n production

# View application logs
kubectl logs -f deployment/user-service -n production

# Port forward to services
kubectl port-forward service/grafana 3000:3000 -n monitoring
```

## Security Considerations

### Secrets Management

1. **Never commit real secrets** to version control
2. Use **Sealed Secrets** for GitOps workflows
3. Rotate secrets regularly using automation
4. Use **External Secrets Operator** for cloud secret managers

### Network Security

1. **Default Deny**: All traffic blocked by default
2. **Explicit Allow**: Only required communication paths
3. **Ingress Control**: Kong with proper authentication
4. **Service Mesh**: mTLS for service-to-service communication

### Compliance

1. **PCI DSS**: Special handling for payment service
2. **SOC 2**: Audit logging and access controls
3. **GDPR**: Data privacy and retention policies
4. **HIPAA**: If handling health data (future)

## Disaster Recovery

### Backup Strategy

1. **Database Backups**: Automated with Velero
2. **Persistent Volume Snapshots**: EBS snapshots
3. **Configuration Backup**: GitOps repository
4. **Cross-Region Replication**: For critical data

### Recovery Procedures

1. **Application Recovery**: ArgoCD sync from Git
2. **Database Recovery**: Point-in-time recovery from backups
3. **Infrastructure Recovery**: Terraform + Kubernetes manifests
4. **Cross-Region Failover**: DNS-based traffic routing

## Performance Optimization

### Resource Tuning

1. **Right-sizing**: Use VPA recommendations
2. **Auto-scaling**: Configure HPA with appropriate metrics
3. **Node Selection**: Use node affinity for workload placement
4. **Quality of Service**: Set appropriate resource requests/limits

### Application Optimization

1. **Database Connection Pooling**: Configured in services
2. **Cache Strategy**: Redis for session and data caching
3. **CDN Integration**: For static content delivery
4. **Asynchronous Processing**: Kafka for event-driven architecture

## Contributing

1. **Testing**: All changes must be tested in development first
2. **Code Review**: Pull requests require platform team approval
3. **Documentation**: Update this README for significant changes
4. **Security Review**: Security team review for production changes

## Support

- **Platform Team**: platform@enterprise.com
- **On-call**: +1-555-ONCALL (24/7 for production issues)
- **Documentation**: https://docs.enterprise.com/infrastructure
- **Runbooks**: https://runbooks.enterprise.com