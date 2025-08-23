#!/bin/bash
# Deploy services to Kubernetes using Helm

set -euo pipefail

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Arguments
ENVIRONMENT="${1:-dev}"
VERSION="${2:-latest}"
SERVICE="${3:-all}"

# Configuration
HELM_TIMEOUT="10m"
HELM_WAIT="true"
ROLLOUT_TIMEOUT="600s"

# Kubernetes namespaces
declare -A NAMESPACES=(
    ["dev"]="lp-dev"
    ["staging"]="lp-staging"
    ["prod"]="lp-prod"
)

# Service configuration
declare -A SERVICES=(
    ["gateway"]="api-gateway"
    ["user-service"]="user-service"
    ["content-service"]="content-service"
    ["analytics-service"]="analytics-service"
    ["payment-service"]="payment-service"
    ["search-service"]="search-service"
    ["recommendation-service"]="recommendation-service"
    ["media-service"]="media-service"
    ["notification-service"]="notification-service"
)

# Environment-specific values
declare -A VALUES_FILES=(
    ["dev"]="values-dev.yaml"
    ["staging"]="values-staging.yaml"
    ["prod"]="values-prod.yaml"
)

# Get namespace
NAMESPACE="${NAMESPACES[$ENVIRONMENT]}"
VALUES_FILE="${VALUES_FILES[$ENVIRONMENT]}"

echo -e "${BLUE}Deploying to ${ENVIRONMENT} environment (namespace: ${NAMESPACE})${NC}"
echo -e "${BLUE}Version: ${VERSION}${NC}"

# Function to check prerequisites
check_prerequisites() {
    echo -e "${YELLOW}Checking prerequisites...${NC}"
    
    # Check kubectl connectivity
    if ! kubectl cluster-info &> /dev/null; then
        echo -e "${RED}Cannot connect to Kubernetes cluster${NC}"
        exit 1
    fi
    
    # Check namespace exists
    if ! kubectl get namespace ${NAMESPACE} &> /dev/null; then
        echo -e "${YELLOW}Creating namespace ${NAMESPACE}...${NC}"
        kubectl create namespace ${NAMESPACE}
    fi
    
    # Check Helm
    if ! command -v helm &> /dev/null; then
        echo -e "${RED}Helm is not installed${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}Prerequisites check passed${NC}"
}

# Function to create secrets
create_secrets() {
    local service=$1
    echo -e "${YELLOW}Creating secrets for ${service}...${NC}"
    
    # Database credentials
    kubectl create secret generic ${service}-db-secret \
        --namespace=${NAMESPACE} \
        --from-literal=username="${DB_USER}" \
        --from-literal=password="${DB_PASSWORD}" \
        --dry-run=client -o yaml | kubectl apply -f -
    
    # API keys
    kubectl create secret generic ${service}-api-keys \
        --namespace=${NAMESPACE} \
        --from-literal=jwt-secret="${JWT_SECRET}" \
        --from-literal=api-key="${API_KEY}" \
        --dry-run=client -o yaml | kubectl apply -f -
    
    # Docker registry credentials
    kubectl create secret docker-registry regcred \
        --namespace=${NAMESPACE} \
        --docker-server="${CI_REGISTRY}" \
        --docker-username="${CI_REGISTRY_USER}" \
        --docker-password="${CI_REGISTRY_PASSWORD}" \
        --dry-run=client -o yaml | kubectl apply -f -
}

# Function to deploy a service
deploy_service() {
    local service=$1
    local chart_name="${SERVICES[$service]}"
    
    echo -e "${BLUE}Deploying ${service}...${NC}"
    
    # Create/update secrets
    create_secrets ${service}
    
    # Set image repository and tag
    IMAGE_REPO="${CI_REGISTRY_IMAGE}/${service}"
    IMAGE_TAG="${VERSION}"
    
    # Deploy using Helm
    helm upgrade --install ${chart_name} \
        ./charts/${service} \
        --namespace ${NAMESPACE} \
        --create-namespace \
        --values ./charts/${service}/${VALUES_FILE} \
        --set image.repository=${IMAGE_REPO} \
        --set image.tag=${IMAGE_TAG} \
        --set image.pullPolicy=Always \
        --set environment=${ENVIRONMENT} \
        --set-string podAnnotations."deployment\.date"="$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
        --set-string podAnnotations."deployed\.by"="${GITLAB_USER_LOGIN:-ci}" \
        --set-string podAnnotations."commit\.sha"="${CI_COMMIT_SHA}" \
        --wait=${HELM_WAIT} \
        --timeout=${HELM_TIMEOUT} \
        --atomic
    
    # Wait for rollout to complete
    echo -e "${YELLOW}Waiting for ${service} rollout to complete...${NC}"
    kubectl rollout status deployment/${chart_name} \
        --namespace=${NAMESPACE} \
        --timeout=${ROLLOUT_TIMEOUT}
    
    # Verify deployment
    verify_deployment ${service} ${chart_name}
}

# Function to verify deployment
verify_deployment() {
    local service=$1
    local deployment=$2
    
    echo -e "${YELLOW}Verifying ${service} deployment...${NC}"
    
    # Check pod status
    READY_PODS=$(kubectl get deployment ${deployment} -n ${NAMESPACE} -o jsonpath='{.status.readyReplicas}')
    DESIRED_PODS=$(kubectl get deployment ${deployment} -n ${NAMESPACE} -o jsonpath='{.spec.replicas}')
    
    if [ "${READY_PODS}" -eq "${DESIRED_PODS}" ]; then
        echo -e "${GREEN}✓ ${service}: ${READY_PODS}/${DESIRED_PODS} pods ready${NC}"
    else
        echo -e "${RED}✗ ${service}: Only ${READY_PODS}/${DESIRED_PODS} pods ready${NC}"
        return 1
    fi
    
    # Check service endpoint
    SERVICE_IP=$(kubectl get svc ${deployment} -n ${NAMESPACE} -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "")
    if [ -n "${SERVICE_IP}" ]; then
        echo -e "${GREEN}✓ Service IP: ${SERVICE_IP}${NC}"
    fi
    
    # Health check
    if [ "${ENVIRONMENT}" != "prod" ]; then
        perform_health_check ${service}
    fi
}

# Function to perform health check
perform_health_check() {
    local service=$1
    local max_attempts=30
    local attempt=1
    
    echo -e "${YELLOW}Performing health check for ${service}...${NC}"
    
    # Get service URL
    if [ "${ENVIRONMENT}" = "dev" ]; then
        SERVICE_URL="https://${service}-dev.lp-platform.com"
    elif [ "${ENVIRONMENT}" = "staging" ]; then
        SERVICE_URL="https://${service}-staging.lp-platform.com"
    else
        SERVICE_URL="https://api.lp-platform.com/${service}"
    fi
    
    # Wait for service to be ready
    while [ $attempt -le $max_attempts ]; do
        if curl -sf "${SERVICE_URL}/health" > /dev/null 2>&1; then
            echo -e "${GREEN}✓ Health check passed for ${service}${NC}"
            return 0
        fi
        
        echo -e "${YELLOW}Waiting for ${service} to be ready... (${attempt}/${max_attempts})${NC}"
        sleep 10
        attempt=$((attempt + 1))
    done
    
    echo -e "${RED}✗ Health check failed for ${service}${NC}"
    return 1
}

# Function to deploy all services
deploy_all_services() {
    echo -e "${BLUE}Deploying all services...${NC}"
    
    # Deploy in dependency order
    local deployment_order=(
        "gateway"
        "user-service"
        "content-service"
        "search-service"
        "analytics-service"
        "recommendation-service"
        "payment-service"
        "media-service"
        "notification-service"
    )
    
    for service in "${deployment_order[@]}"; do
        if deploy_service ${service}; then
            echo -e "${GREEN}✓ ${service} deployed successfully${NC}"
        else
            echo -e "${RED}✗ Failed to deploy ${service}${NC}"
            exit 1
        fi
    done
}

# Function to update ingress
update_ingress() {
    echo -e "${YELLOW}Updating ingress configuration...${NC}"
    
    # Apply ingress configuration
    kubectl apply -f ./k8s/ingress/${ENVIRONMENT}-ingress.yaml -n ${NAMESPACE}
    
    # Update SSL certificates
    if [ "${ENVIRONMENT}" = "prod" ]; then
        kubectl apply -f ./k8s/certificates/prod-certificates.yaml -n ${NAMESPACE}
    fi
}

# Function to run post-deployment tasks
post_deployment_tasks() {
    echo -e "${YELLOW}Running post-deployment tasks...${NC}"
    
    # Run database migrations
    if [ -f "./migrations/run-migrations.sh" ]; then
        ./migrations/run-migrations.sh ${ENVIRONMENT}
    fi
    
    # Clear caches
    kubectl exec -n ${NAMESPACE} deployment/redis-master -- redis-cli FLUSHALL
    
    # Warm up caches
    if [ "${ENVIRONMENT}" != "prod" ]; then
        curl -X POST "https://api-${ENVIRONMENT}.lp-platform.com/admin/cache/warmup"
    fi
    
    # Send deployment notification
    send_deployment_notification
}

# Function to send deployment notification
send_deployment_notification() {
    if [ -n "${SLACK_WEBHOOK_URL:-}" ]; then
        curl -X POST -H 'Content-type: application/json' \
            --data "{
                \"text\":\"Deployment completed\",
                \"attachments\":[{
                    \"color\":\"good\",
                    \"title\":\"Deployment to ${ENVIRONMENT}\",
                    \"fields\":[
                        {\"title\":\"Version\",\"value\":\"${VERSION}\",\"short\":true},
                        {\"title\":\"Service\",\"value\":\"${SERVICE}\",\"short\":true},
                        {\"title\":\"Deployed by\",\"value\":\"${GITLAB_USER_LOGIN:-CI}\",\"short\":true},
                        {\"title\":\"Time\",\"value\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",\"short\":true}
                    ]
                }]
            }" \
            ${SLACK_WEBHOOK_URL}
    fi
}

# Function to generate deployment report
generate_deployment_report() {
    echo -e "${YELLOW}Generating deployment report...${NC}"
    
    REPORT_FILE="deployment-report-${ENVIRONMENT}-$(date +%Y%m%d-%H%M%S).json"
    
    # Collect deployment information
    kubectl get deployments -n ${NAMESPACE} -o json > ${REPORT_FILE}
    
    # Add summary
    cat >> ${REPORT_FILE} <<EOF
{
    "summary": {
        "environment": "${ENVIRONMENT}",
        "version": "${VERSION}",
        "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
        "deployed_by": "${GITLAB_USER_LOGIN:-CI}",
        "commit": "${CI_COMMIT_SHA:-unknown}"
    }
}
EOF
    
    echo -e "${GREEN}Deployment report saved to ${REPORT_FILE}${NC}"
}

# Main execution
main() {
    check_prerequisites
    
    if [ "${SERVICE}" = "all" ]; then
        deploy_all_services
    else
        if [[ -v SERVICES[$SERVICE] ]]; then
            deploy_service ${SERVICE}
        else
            echo -e "${RED}Unknown service: ${SERVICE}${NC}"
            echo "Available services: ${!SERVICES[@]}"
            exit 1
        fi
    fi
    
    update_ingress
    post_deployment_tasks
    generate_deployment_report
    
    echo -e "${GREEN}Deployment completed successfully!${NC}"
}

# Run main function
main