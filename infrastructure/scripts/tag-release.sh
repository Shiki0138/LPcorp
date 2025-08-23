#!/bin/bash
set -e

# Tag Release Script
# This script tags Docker images for release

echo "============================================"
echo "Tagging Enterprise System Release"
echo "============================================"

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
REGISTRY="${DOCKER_REGISTRY:-localhost:5000}"
CURRENT_TAG="${CURRENT_TAG:-latest}"
RELEASE_TAG="${RELEASE_TAG}"
GIT_TAG="${GIT_TAG:-true}"

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

# Validate inputs
if [ -z "$RELEASE_TAG" ]; then
    print_error "RELEASE_TAG is required"
    echo "Usage: RELEASE_TAG=v1.0.0 $0"
    exit 1
fi

# Validate tag format (semantic versioning)
if ! [[ "$RELEASE_TAG" =~ ^v?[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9]+)?$ ]]; then
    print_error "Invalid release tag format. Expected semantic versioning (e.g., v1.0.0, 1.2.3, v1.0.0-beta)"
    exit 1
fi

# List of images to tag
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

# Function to check if image exists
check_image_exists() {
    local image=$1
    docker image inspect "$image" > /dev/null 2>&1
}

# Function to tag an image
tag_image() {
    local image=$1
    local source_tag="${REGISTRY}/${image}:${CURRENT_TAG}"
    local target_tag="${REGISTRY}/${image}:${RELEASE_TAG}"
    
    print_status "Tagging ${image}..."
    
    # Check if source image exists
    if ! check_image_exists "$source_tag"; then
        print_error "Source image not found: $source_tag"
        return 1
    fi
    
    # Create release tag
    docker tag "$source_tag" "$target_tag" || {
        print_error "Failed to tag $image"
        return 1
    }
    
    # Also create major and minor version tags
    if [[ "$RELEASE_TAG" =~ ^v?([0-9]+)\.([0-9]+)\.([0-9]+) ]]; then
        local major="${BASH_REMATCH[1]}"
        local minor="${BASH_REMATCH[2]}"
        local patch="${BASH_REMATCH[3]}"
        
        # Tag with major version (e.g., v1)
        docker tag "$source_tag" "${REGISTRY}/${image}:v${major}"
        
        # Tag with major.minor version (e.g., v1.0)
        docker tag "$source_tag" "${REGISTRY}/${image}:v${major}.${minor}"
    fi
    
    print_status "Successfully tagged ${image}"
    return 0
}

# Start tagging process
print_status "Starting release tagging process..."
print_info "Current tag: ${CURRENT_TAG}"
print_info "Release tag: ${RELEASE_TAG}"
print_info "Registry: ${REGISTRY}"

# Track results
TAGGED_COUNT=0
FAILED_COUNT=0

# Tag all images
for image in "${IMAGES[@]}"; do
    if tag_image "$image"; then
        ((TAGGED_COUNT++))
    else
        ((FAILED_COUNT++))
    fi
done

# List all tagged images
print_status "Listing tagged images..."
docker images | grep -E "${REGISTRY}.*${RELEASE_TAG}" || true

# Create Git tag if requested
if [ "$GIT_TAG" = "true" ] && [ -d "${PROJECT_ROOT}/.git" ]; then
    print_status "Creating Git tag..."
    
    # Check if tag already exists
    if git rev-parse "$RELEASE_TAG" >/dev/null 2>&1; then
        print_warning "Git tag $RELEASE_TAG already exists"
    else
        # Create annotated tag
        git tag -a "$RELEASE_TAG" -m "Release $RELEASE_TAG

Tagged Docker images:
$(for img in "${IMAGES[@]}"; do echo "- ${img}:${RELEASE_TAG}"; done)

Automated release by tag-release.sh" || {
            print_error "Failed to create Git tag"
        }
        
        print_status "Created Git tag: $RELEASE_TAG"
        print_info "Push tag to remote with: git push origin $RELEASE_TAG"
    fi
fi

# Generate release notes template
if [ "$GENERATE_RELEASE_NOTES" = "true" ]; then
    RELEASE_NOTES_FILE="${PROJECT_ROOT}/RELEASE_NOTES_${RELEASE_TAG}.md"
    
    print_status "Generating release notes template..."
    
    cat > "$RELEASE_NOTES_FILE" << EOF
# Release Notes - ${RELEASE_TAG}

Release Date: $(date +'%Y-%m-%d')

## Overview
Brief description of this release...

## New Features
- Feature 1
- Feature 2

## Improvements
- Improvement 1
- Improvement 2

## Bug Fixes
- Fix 1
- Fix 2

## Breaking Changes
- None

## Docker Images
The following Docker images are included in this release:

| Image | Tag |
|-------|-----|
$(for img in "${IMAGES[@]}"; do echo "| ${img} | ${RELEASE_TAG} |"; done)

## Deployment Instructions
1. Pull the new images:
   \`\`\`bash
   docker-compose pull
   \`\`\`

2. Stop the current services:
   \`\`\`bash
   docker-compose down
   \`\`\`

3. Start with new images:
   \`\`\`bash
   docker-compose up -d
   \`\`\`

## Rollback Instructions
To rollback to the previous version:
\`\`\`bash
RELEASE_TAG=<previous-version> docker-compose up -d
\`\`\`

## Known Issues
- None

## Contributors
- Generated by automated release process
EOF

    print_status "Release notes template created: $RELEASE_NOTES_FILE"
fi

# Create Docker Compose override for release
if [ "$CREATE_COMPOSE_OVERRIDE" = "true" ]; then
    COMPOSE_OVERRIDE="${PROJECT_ROOT}/docker-compose.${RELEASE_TAG}.yml"
    
    print_status "Creating Docker Compose override file..."
    
    cat > "$COMPOSE_OVERRIDE" << EOF
# Docker Compose Override for Release ${RELEASE_TAG}
# Use with: docker-compose -f docker-compose.yml -f docker-compose.${RELEASE_TAG}.yml up -d

version: '3.9'

services:
EOF

    for image in "${IMAGES[@]}"; do
        # Skip base images
        if [[ "$image" == *"-base" ]]; then
            continue
        fi
        
        # Convert image name to service name (remove -service suffix if present)
        service_name="${image%-service}"
        if [[ "$image" == *"-service" ]]; then
            service_name="${image}"
        fi
        
        cat >> "$COMPOSE_OVERRIDE" << EOF
  ${service_name}:
    image: ${REGISTRY}/${image}:${RELEASE_TAG}
EOF
    done
    
    print_status "Docker Compose override created: $COMPOSE_OVERRIDE"
fi

# Summary
echo ""
echo "============================================"
echo "Release Tagging Summary"
echo "============================================"
echo "Release Tag: ${RELEASE_TAG}"
echo "Images Tagged: ${TAGGED_COUNT}"
echo "Failed: ${FAILED_COUNT}"
echo "Git Tag Created: $([ "$GIT_TAG" = "true" ] && echo "Yes" || echo "No")"
echo "============================================"

# Next steps
echo ""
echo "Next Steps:"
echo "1. Push images to registry:"
echo "   RELEASE_TAG=${RELEASE_TAG} ./push-to-registry.sh"
echo ""
if [ "$GIT_TAG" = "true" ]; then
    echo "2. Push Git tag:"
    echo "   git push origin ${RELEASE_TAG}"
    echo ""
fi
echo "3. Deploy release:"
echo "   RELEASE_TAG=${RELEASE_TAG} docker-compose up -d"

# Exit with appropriate code
if [ $FAILED_COUNT -gt 0 ]; then
    exit 1
else
    exit 0
fi