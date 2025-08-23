#!/bin/bash
set -e

# Build All Docker Images Script
# This script builds all microservice Docker images

echo "============================================"
echo "Building Enterprise System Docker Images"
echo "============================================"

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
REGISTRY="${DOCKER_REGISTRY:-localhost:5000}"
TAG="${VERSION:-latest}"
BUILD_PARALLEL="${BUILD_PARALLEL:-true}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

print_error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR:${NC} $1" >&2
}

print_warning() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING:${NC} $1"
}

# Check Docker daemon
if ! docker info > /dev/null 2>&1; then
    print_error "Docker daemon is not running"
    exit 1
fi

# Build base images first
print_status "Building base images..."

# Java base image
print_status "Building Java base image..."
docker build -t "${REGISTRY}/java-base:${TAG}" \
    -f "${PROJECT_ROOT}/infrastructure/docker/java-base/Dockerfile" \
    "${PROJECT_ROOT}/infrastructure/docker/java-base" || {
    print_error "Failed to build Java base image"
    exit 1
}

# Node base image
print_status "Building Node base image..."
docker build -t "${REGISTRY}/node-base:${TAG}" \
    -f "${PROJECT_ROOT}/infrastructure/docker/node-base/Dockerfile" \
    "${PROJECT_ROOT}/infrastructure/docker/node-base" || {
    print_error "Failed to build Node base image"
    exit 1
}

# List of services to build
SERVICES=(
    "user-service"
    "order-service"
    "inventory-service"
    "payment-service"
    "notification-service"
    "auth-service"
    "api-gateway"
    "analytics-service"
)

# Function to build a service
build_service() {
    local service=$1
    local service_dir="${PROJECT_ROOT}/services/${service}"
    
    if [ ! -d "$service_dir" ]; then
        print_warning "Service directory not found: $service_dir"
        return 1
    fi
    
    print_status "Building ${service}..."
    
    # Update Dockerfile to use local base image
    sed -i.bak "s|FROM infrastructure/docker/java-base:latest|FROM ${REGISTRY}/java-base:${TAG}|g" \
        "${service_dir}/Dockerfile"
    
    # Build the service
    docker build -t "${REGISTRY}/${service}:${TAG}" \
        -f "${service_dir}/Dockerfile" \
        "${service_dir}" || {
        print_error "Failed to build ${service}"
        # Restore original Dockerfile
        mv "${service_dir}/Dockerfile.bak" "${service_dir}/Dockerfile"
        return 1
    }
    
    # Restore original Dockerfile
    mv "${service_dir}/Dockerfile.bak" "${service_dir}/Dockerfile"
    
    # Tag with latest
    docker tag "${REGISTRY}/${service}:${TAG}" "${REGISTRY}/${service}:latest"
    
    print_status "Successfully built ${service}"
    return 0
}

# Build services
if [ "$BUILD_PARALLEL" = "true" ]; then
    print_status "Building services in parallel..."
    
    # Build services in parallel
    for service in "${SERVICES[@]}"; do
        build_service "$service" &
    done
    
    # Wait for all builds to complete
    wait
else
    print_status "Building services sequentially..."
    
    # Build services sequentially
    for service in "${SERVICES[@]}"; do
        build_service "$service"
    done
fi

# List built images
print_status "Listing built images..."
docker images | grep -E "${REGISTRY}|REPOSITORY" | head -20

# Calculate total size
TOTAL_SIZE=$(docker images | grep "${REGISTRY}" | awk '{sum += $7} END {print sum}')
print_status "Total images size: ${TOTAL_SIZE:-0} MB"

# Optional: Export build artifacts
if [ "${EXPORT_IMAGES}" = "true" ]; then
    print_status "Exporting Docker images..."
    mkdir -p "${PROJECT_ROOT}/build/images"
    
    for service in "${SERVICES[@]}"; do
        docker save -o "${PROJECT_ROOT}/build/images/${service}-${TAG}.tar" \
            "${REGISTRY}/${service}:${TAG}"
    done
fi

# Optional: Run security scan
if [ "${SECURITY_SCAN}" = "true" ]; then
    print_status "Running security scans..."
    
    for service in "${SERVICES[@]}"; do
        print_status "Scanning ${service}..."
        docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
            aquasec/trivy:latest image "${REGISTRY}/${service}:${TAG}" || \
            print_warning "Security scan found vulnerabilities in ${service}"
    done
fi

# Summary
echo ""
echo "============================================"
echo "Build Summary"
echo "============================================"
echo "Registry: ${REGISTRY}"
echo "Tag: ${TAG}"
echo "Services built: ${#SERVICES[@]}"
echo "Build mode: $([ "$BUILD_PARALLEL" = "true" ] && echo "Parallel" || echo "Sequential")"
echo "============================================"

print_status "All images built successfully!"

# Optional: Clean up dangling images
if [ "${CLEANUP}" = "true" ]; then
    print_status "Cleaning up dangling images..."
    docker image prune -f
fi