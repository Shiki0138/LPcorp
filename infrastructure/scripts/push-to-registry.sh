#!/bin/bash
set -e

# Push Docker Images to Registry Script
# This script pushes all built images to a Docker registry

echo "============================================"
echo "Pushing Enterprise System Docker Images"
echo "============================================"

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
REGISTRY="${DOCKER_REGISTRY:-localhost:5000}"
TAG="${VERSION:-latest}"
PUSH_PARALLEL="${PUSH_PARALLEL:-true}"
DRY_RUN="${DRY_RUN:-false}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

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

print_info() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] INFO:${NC} $1"
}

# Check Docker daemon
if ! docker info > /dev/null 2>&1; then
    print_error "Docker daemon is not running"
    exit 1
fi

# Login to registry if credentials provided
if [ -n "${REGISTRY_USERNAME}" ] && [ -n "${REGISTRY_PASSWORD}" ]; then
    print_status "Logging in to registry ${REGISTRY}..."
    echo "${REGISTRY_PASSWORD}" | docker login "${REGISTRY}" -u "${REGISTRY_USERNAME}" --password-stdin || {
        print_error "Failed to login to registry"
        exit 1
    }
fi

# List of images to push
IMAGES=(
    "java-base"
    "node-base"
    "user-service"
    "order-service"
    "inventory-service"
    "payment-service"
    "notification-service"
    "auth-service"
    "api-gateway"
    "analytics-service"
)

# Track push results
declare -A PUSH_RESULTS
FAILED_PUSHES=0
SUCCESSFUL_PUSHES=0

# Function to push an image
push_image() {
    local image=$1
    local full_image="${REGISTRY}/${image}:${TAG}"
    
    if [ "$DRY_RUN" = "true" ]; then
        print_info "[DRY RUN] Would push: ${full_image}"
        PUSH_RESULTS["${image}"]="dry-run"
        return 0
    fi
    
    # Check if image exists locally
    if ! docker image inspect "${full_image}" > /dev/null 2>&1; then
        print_warning "Image not found locally: ${full_image}"
        PUSH_RESULTS["${image}"]="not-found"
        ((FAILED_PUSHES++))
        return 1
    fi
    
    print_status "Pushing ${full_image}..."
    
    # Push the image with retry logic
    local retry_count=0
    local max_retries=3
    
    while [ $retry_count -lt $max_retries ]; do
        if docker push "${full_image}"; then
            print_status "Successfully pushed ${image}"
            
            # Also push latest tag
            if [ "${TAG}" != "latest" ]; then
                docker tag "${full_image}" "${REGISTRY}/${image}:latest"
                docker push "${REGISTRY}/${image}:latest" || print_warning "Failed to push latest tag for ${image}"
            fi
            
            PUSH_RESULTS["${image}"]="success"
            ((SUCCESSFUL_PUSHES++))
            return 0
        else
            ((retry_count++))
            if [ $retry_count -lt $max_retries ]; then
                print_warning "Push failed for ${image}, retrying... (${retry_count}/${max_retries})"
                sleep 5
            fi
        fi
    done
    
    print_error "Failed to push ${image} after ${max_retries} attempts"
    PUSH_RESULTS["${image}"]="failed"
    ((FAILED_PUSHES++))
    return 1
}

# Function to check registry connectivity
check_registry() {
    print_status "Checking registry connectivity..."
    
    # Try to reach the registry catalog
    if command -v curl > /dev/null 2>&1; then
        if curl -s -f -k "https://${REGISTRY}/v2/_catalog" > /dev/null 2>&1 || \
           curl -s -f "http://${REGISTRY}/v2/_catalog" > /dev/null 2>&1; then
            print_status "Registry ${REGISTRY} is accessible"
            return 0
        fi
    fi
    
    # Fallback: try docker pull
    if docker pull "${REGISTRY}/hello-world:latest" > /dev/null 2>&1; then
        print_status "Registry ${REGISTRY} is accessible"
        return 0
    fi
    
    print_warning "Cannot verify registry connectivity, proceeding anyway..."
    return 0
}

# Check registry before pushing
check_registry

# Start pushing images
print_status "Starting image push process..."
print_info "Registry: ${REGISTRY}"
print_info "Tag: ${TAG}"
print_info "Images to push: ${#IMAGES[@]}"

# Push images
if [ "$PUSH_PARALLEL" = "true" ] && [ "$DRY_RUN" != "true" ]; then
    print_status "Pushing images in parallel..."
    
    # Push images in parallel
    for image in "${IMAGES[@]}"; do
        push_image "$image" &
    done
    
    # Wait for all pushes to complete
    wait
else
    print_status "Pushing images sequentially..."
    
    # Push images sequentially
    for image in "${IMAGES[@]}"; do
        push_image "$image"
    done
fi

# Generate push report
print_status "Generating push report..."

echo ""
echo "============================================"
echo "Push Report"
echo "============================================"
printf "%-30s %-15s\n" "IMAGE" "STATUS"
printf "%-30s %-15s\n" "-----" "------"

for image in "${IMAGES[@]}"; do
    status="${PUSH_RESULTS[$image]:-unknown}"
    case $status in
        "success")
            printf "%-30s ${GREEN}%-15s${NC}\n" "$image" "$status"
            ;;
        "failed"|"not-found")
            printf "%-30s ${RED}%-15s${NC}\n" "$image" "$status"
            ;;
        "dry-run")
            printf "%-30s ${BLUE}%-15s${NC}\n" "$image" "$status"
            ;;
        *)
            printf "%-30s ${YELLOW}%-15s${NC}\n" "$image" "$status"
            ;;
    esac
done

echo "============================================"
echo "Summary:"
echo "  Successful: ${SUCCESSFUL_PUSHES}"
echo "  Failed: ${FAILED_PUSHES}"
echo "  Total: ${#IMAGES[@]}"
echo "============================================"

# Create manifest for multi-arch support (optional)
if [ "${CREATE_MANIFEST}" = "true" ] && [ "$DRY_RUN" != "true" ]; then
    print_status "Creating multi-arch manifests..."
    
    for image in "${IMAGES[@]}"; do
        if [ "${PUSH_RESULTS[$image]}" = "success" ]; then
            # Assuming we have arm64 and amd64 images
            docker manifest create "${REGISTRY}/${image}:${TAG}" \
                "${REGISTRY}/${image}:${TAG}-amd64" \
                "${REGISTRY}/${image}:${TAG}-arm64" 2>/dev/null || \
                print_warning "Failed to create manifest for ${image}"
            
            docker manifest push "${REGISTRY}/${image}:${TAG}" 2>/dev/null || \
                print_warning "Failed to push manifest for ${image}"
        fi
    done
fi

# Logout from registry
if [ -n "${REGISTRY_USERNAME}" ]; then
    docker logout "${REGISTRY}" 2>/dev/null || true
fi

# Exit with appropriate code
if [ $FAILED_PUSHES -gt 0 ]; then
    print_error "Push completed with ${FAILED_PUSHES} failures"
    exit 1
else
    print_status "All images pushed successfully!"
    exit 0
fi