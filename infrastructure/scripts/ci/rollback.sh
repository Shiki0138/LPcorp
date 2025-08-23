#!/bin/bash
# Rollback script for Kubernetes deployments

set -euo pipefail

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Arguments
ENVIRONMENT="${1:-prod}"
SERVICE="${2:-all}"
REVISION="${3:-}"

# Configuration
ROLLBACK_TIMEOUT="10m"

# Kubernetes namespaces
declare -A NAMESPACES=(
    ["dev"]="lp-dev"
    ["staging"]="lp-staging"
    ["prod"]="lp-prod"
)

# Service mapping
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

NAMESPACE="${NAMESPACES[$ENVIRONMENT]}"

echo -e "${BLUE}Rollback Configuration${NC}"
echo -e "${BLUE}Environment: ${ENVIRONMENT}${NC}"
echo -e "${BLUE}Namespace: ${NAMESPACE}${NC}"
echo -e "${BLUE}Service: ${SERVICE}${NC}"
echo -e "${BLUE}Revision: ${REVISION:-auto}${NC}"

# Function to get deployment history
get_deployment_history() {
    local service=$1
    local chart_name="${SERVICES[$service]}"
    
    echo -e "${YELLOW}Deployment history for ${service}:${NC}"
    helm history ${chart_name} -n ${NAMESPACE} --max 10
}

# Function to get current revision
get_current_revision() {
    local service=$1
    local chart_name="${SERVICES[$service]}"
    
    helm list -n ${NAMESPACE} -o json | \
        jq -r ".[] | select(.name==\"${chart_name}\") | .revision"
}

# Function to perform rollback
rollback_service() {
    local service=$1
    local chart_name="${SERVICES[$service]}"
    local target_revision=$2
    
    echo -e "${YELLOW}Rolling back ${service}...${NC}"
    
    # Get current revision if target not specified
    if [ -z "${target_revision}" ]; then
        CURRENT_REVISION=$(get_current_revision ${service})
        target_revision=$((CURRENT_REVISION - 1))
        echo -e "${YELLOW}Auto-detected rollback to revision ${target_revision}${NC}"
    fi
    
    # Confirm rollback
    if [ "${ENVIRONMENT}" = "prod" ]; then
        echo -e "${RED}WARNING: This will rollback ${service} in PRODUCTION!${NC}"
        echo -n "Type 'ROLLBACK' to confirm: "
        read confirmation
        if [ "${confirmation}" != "ROLLBACK" ]; then
            echo -e "${RED}Rollback cancelled${NC}"
            exit 1
        fi
    fi
    
    # Create backup of current state
    kubectl get all -n ${NAMESPACE} -l app=${chart_name} -o yaml > \
        "backup-${service}-$(date +%Y%m%d-%H%M%S).yaml"
    
    # Perform Helm rollback
    helm rollback ${chart_name} ${target_revision} \
        --namespace ${NAMESPACE} \
        --wait \
        --timeout ${ROLLBACK_TIMEOUT}
    
    # Wait for rollout to complete
    echo -e "${YELLOW}Waiting for rollout to complete...${NC}"
    kubectl rollout status deployment/${chart_name} \
        --namespace=${NAMESPACE} \
        --timeout=${ROLLBACK_TIMEOUT}
    
    # Verify rollback
    verify_rollback ${service} ${chart_name}
}

# Function to verify rollback
verify_rollback() {
    local service=$1
    local deployment=$2
    
    echo -e "${YELLOW}Verifying rollback for ${service}...${NC}"
    
    # Check pod status
    READY_PODS=$(kubectl get deployment ${deployment} -n ${NAMESPACE} -o jsonpath='{.status.readyReplicas}')
    DESIRED_PODS=$(kubectl get deployment ${deployment} -n ${NAMESPACE} -o jsonpath='{.spec.replicas}')
    
    if [ "${READY_PODS}" -eq "${DESIRED_PODS}" ]; then
        echo -e "${GREEN}✓ ${service}: ${READY_PODS}/${DESIRED_PODS} pods ready${NC}"
    else
        echo -e "${RED}✗ ${service}: Only ${READY_PODS}/${DESIRED_PODS} pods ready${NC}"
        return 1
    fi
    
    # Health check
    perform_health_check ${service}
}

# Function to perform health check
perform_health_check() {
    local service=$1
    local max_attempts=30
    local attempt=1
    
    echo -e "${YELLOW}Performing health check for ${service}...${NC}"
    
    # Construct health check URL
    if [ "${ENVIRONMENT}" = "prod" ]; then
        HEALTH_URL="https://api.lp-platform.com/${service}/health"
    else
        HEALTH_URL="https://${service}-${ENVIRONMENT}.lp-platform.com/health"
    fi
    
    while [ $attempt -le $max_attempts ]; do
        if curl -sf "${HEALTH_URL}" > /dev/null 2>&1; then
            echo -e "${GREEN}✓ Health check passed${NC}"
            return 0
        fi
        
        echo -e "${YELLOW}Waiting for service to be healthy... (${attempt}/${max_attempts})${NC}"
        sleep 10
        attempt=$((attempt + 1))
    done
    
    echo -e "${RED}✗ Health check failed${NC}"
    return 1
}

# Function to rollback all services
rollback_all_services() {
    echo -e "${BLUE}Rolling back all services...${NC}"
    
    # Rollback in reverse dependency order
    local rollback_order=(
        "notification-service"
        "media-service"
        "payment-service"
        "recommendation-service"
        "analytics-service"
        "search-service"
        "content-service"
        "user-service"
        "gateway"
    )
    
    for service in "${rollback_order[@]}"; do
        if rollback_service ${service} ""; then
            echo -e "${GREEN}✓ ${service} rolled back successfully${NC}"
        else
            echo -e "${RED}✗ Failed to rollback ${service}${NC}"
            # Continue with other services
        fi
    done
}

# Function to emergency rollback
emergency_rollback() {
    echo -e "${RED}EMERGENCY ROLLBACK INITIATED${NC}"
    
    # Scale down problematic deployments
    for service in "${!SERVICES[@]}"; do
        deployment="${SERVICES[$service]}"
        echo -e "${YELLOW}Scaling down ${deployment}...${NC}"
        kubectl scale deployment/${deployment} --replicas=0 -n ${NAMESPACE} || true
    done
    
    # Wait for pods to terminate
    sleep 30
    
    # Rollback to last known good configuration
    for service in "${!SERVICES[@]}"; do
        rollback_service ${service} "" || true
    done
}

# Function to generate rollback report
generate_rollback_report() {
    local report_file="rollback-report-${ENVIRONMENT}-$(date +%Y%m%d-%H%M%S).json"
    
    echo -e "${YELLOW}Generating rollback report...${NC}"
    
    # Collect current state
    cat > ${report_file} <<EOF
{
    "rollback_info": {
        "environment": "${ENVIRONMENT}",
        "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
        "initiated_by": "${USER}",
        "service": "${SERVICE}",
        "target_revision": "${REVISION:-auto}"
    },
    "deployments": $(kubectl get deployments -n ${NAMESPACE} -o json),
    "pods": $(kubectl get pods -n ${NAMESPACE} -o json)
}
EOF
    
    echo -e "${GREEN}Rollback report saved to ${report_file}${NC}"
}

# Function to send rollback notification
send_rollback_notification() {
    local status=$1
    
    if [ -n "${SLACK_WEBHOOK_URL:-}" ]; then
        local color="good"
        if [ "${status}" != "success" ]; then
            color="danger"
        fi
        
        curl -X POST -H 'Content-type: application/json' \
            --data "{
                \"text\":\"Rollback ${status}\",
                \"attachments\":[{
                    \"color\":\"${color}\",
                    \"title\":\"Rollback in ${ENVIRONMENT}\",
                    \"fields\":[
                        {\"title\":\"Service\",\"value\":\"${SERVICE}\",\"short\":true},
                        {\"title\":\"Status\",\"value\":\"${status}\",\"short\":true},
                        {\"title\":\"Initiated by\",\"value\":\"${USER}\",\"short\":true},
                        {\"title\":\"Time\",\"value\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",\"short\":true}
                    ]
                }]
            }" \
            ${SLACK_WEBHOOK_URL}
    fi
}

# Function to cleanup after rollback
cleanup_rollback() {
    echo -e "${YELLOW}Cleaning up after rollback...${NC}"
    
    # Clear application caches
    kubectl exec -n ${NAMESPACE} deployment/redis-master -- redis-cli FLUSHALL || true
    
    # Restart dependent services if needed
    if [ "${SERVICE}" = "gateway" ] || [ "${SERVICE}" = "all" ]; then
        echo -e "${YELLOW}Restarting dependent services...${NC}"
        kubectl rollout restart deployment -n ${NAMESPACE} -l tier=backend
    fi
}

# Main execution
main() {
    # Check if emergency rollback
    if [ "${SERVICE}" = "emergency" ]; then
        emergency_rollback
        generate_rollback_report
        send_rollback_notification "emergency"
        exit 0
    fi
    
    # Show current state
    echo -e "${BLUE}Current deployment state:${NC}"
    helm list -n ${NAMESPACE}
    
    # Perform rollback
    if [ "${SERVICE}" = "all" ]; then
        rollback_all_services
    else
        if [[ -v SERVICES[$SERVICE] ]]; then
            get_deployment_history ${SERVICE}
            rollback_service ${SERVICE} "${REVISION}"
        else
            echo -e "${RED}Unknown service: ${SERVICE}${NC}"
            echo "Available services: ${!SERVICES[@]} all emergency"
            exit 1
        fi
    fi
    
    # Post-rollback tasks
    cleanup_rollback
    generate_rollback_report
    send_rollback_notification "success"
    
    echo -e "${GREEN}Rollback completed successfully!${NC}"
}

# Error handling
trap 'echo -e "${RED}Rollback failed!${NC}"; send_rollback_notification "failed"; exit 1' ERR

# Run main function
main