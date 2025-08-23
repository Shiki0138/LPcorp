#!/bin/bash
# Run tests based on test suite and environment

set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Arguments
TEST_SUITE="${1:-unit}"
ENVIRONMENT="${2:-dev}"

# Test configuration
TEST_TIMEOUT="30m"
RETRY_COUNT=3
PARALLEL_JOBS=4

echo -e "${YELLOW}Running ${TEST_SUITE} tests in ${ENVIRONMENT} environment${NC}"

# Function to run tests with retry
run_with_retry() {
    local command=$1
    local attempt=1
    
    while [ $attempt -le $RETRY_COUNT ]; do
        echo -e "${YELLOW}Attempt $attempt of $RETRY_COUNT${NC}"
        
        if eval "$command"; then
            echo -e "${GREEN}Tests passed!${NC}"
            return 0
        else
            echo -e "${RED}Test failed on attempt $attempt${NC}"
            attempt=$((attempt + 1))
            if [ $attempt -le $RETRY_COUNT ]; then
                echo "Retrying in 10 seconds..."
                sleep 10
            fi
        fi
    done
    
    return 1
}

# Unit tests
run_unit_tests() {
    echo -e "${YELLOW}Running unit tests...${NC}"
    
    # Java services
    if [ -f "pom.xml" ]; then
        run_with_retry "mvn test -T ${PARALLEL_JOBS}"
    fi
    
    # Node.js services
    if [ -f "package.json" ]; then
        run_with_retry "npm test -- --coverage"
    fi
    
    # Python services
    if [ -f "requirements.txt" ]; then
        run_with_retry "pytest --cov=. --cov-report=xml --cov-report=html"
    fi
    
    # Go services
    if [ -f "go.mod" ]; then
        run_with_retry "go test -v -race -coverprofile=coverage.out ./..."
    fi
}

# Integration tests
run_integration_tests() {
    echo -e "${YELLOW}Running integration tests...${NC}"
    
    # Start test containers
    docker-compose -f docker-compose.test.yml up -d
    
    # Wait for services to be ready
    sleep 30
    
    # Run integration tests
    if [ -f "pom.xml" ]; then
        run_with_retry "mvn verify -Pintegration-test -Dtest.environment=${ENVIRONMENT}"
    fi
    
    if [ -f "package.json" ]; then
        run_with_retry "npm run test:integration"
    fi
    
    # Cleanup
    docker-compose -f docker-compose.test.yml down
}

# API tests
run_api_tests() {
    echo -e "${YELLOW}Running API tests...${NC}"
    
    # Set API endpoint based on environment
    case $ENVIRONMENT in
        dev)
            API_URL="https://api-dev.lp-platform.com"
            ;;
        staging)
            API_URL="https://api-staging.lp-platform.com"
            ;;
        prod)
            API_URL="https://api.lp-platform.com"
            ;;
    esac
    
    # Run Newman tests
    if [ -f "postman_collection.json" ]; then
        run_with_retry "newman run postman_collection.json \
            --environment postman_environment_${ENVIRONMENT}.json \
            --global-var baseUrl=${API_URL} \
            --reporters cli,junit \
            --reporter-junit-export results/newman-results.xml"
    fi
    
    # Run REST Assured tests
    if [ -f "pom.xml" ] && grep -q "rest-assured" pom.xml; then
        run_with_retry "mvn test -Papi-test -Dapi.base.url=${API_URL}"
    fi
}

# E2E tests
run_e2e_tests() {
    echo -e "${YELLOW}Running E2E tests...${NC}"
    
    # Cypress tests
    if [ -d "cypress" ]; then
        export CYPRESS_baseUrl="https://${ENVIRONMENT}.lp-platform.com"
        run_with_retry "npm run cypress:run"
    fi
    
    # Selenium tests
    if [ -f "pom.xml" ] && grep -q "selenium" pom.xml; then
        run_with_retry "mvn test -Pe2e-test -Dselenium.base.url=https://${ENVIRONMENT}.lp-platform.com"
    fi
}

# Performance tests
run_performance_tests() {
    echo -e "${YELLOW}Running performance tests...${NC}"
    
    # K6 tests
    if [ -f "k6-script.js" ]; then
        run_with_retry "k6 run \
            --vus 100 \
            --duration 5m \
            --out json=performance-results.json \
            k6-script.js"
    fi
    
    # JMeter tests
    if [ -f "test-plan.jmx" ]; then
        run_with_retry "jmeter -n -t test-plan.jmx \
            -l results.jtl \
            -e -o jmeter-report"
    fi
}

# Security tests
run_security_tests() {
    echo -e "${YELLOW}Running security tests...${NC}"
    
    # OWASP ZAP scan
    if command -v zap-cli &> /dev/null; then
        run_with_retry "zap-cli quick-scan \
            --self-contained \
            --start-options '-config api.disablekey=true' \
            https://${ENVIRONMENT}.lp-platform.com"
    fi
    
    # Dependency check
    if [ -f "pom.xml" ]; then
        run_with_retry "mvn dependency-check:check"
    fi
}

# Contract tests
run_contract_tests() {
    echo -e "${YELLOW}Running contract tests...${NC}"
    
    # Pact tests
    if [ -f "pacts" ]; then
        run_with_retry "pact verify \
            --provider-base-url https://${ENVIRONMENT}.lp-platform.com \
            --pact-broker-url ${PACT_BROKER_URL}"
    fi
}

# Main execution
case $TEST_SUITE in
    unit)
        run_unit_tests
        ;;
    integration)
        run_integration_tests
        ;;
    api)
        run_api_tests
        ;;
    e2e)
        run_e2e_tests
        ;;
    performance)
        run_performance_tests
        ;;
    security)
        run_security_tests
        ;;
    contract)
        run_contract_tests
        ;;
    all)
        run_unit_tests
        run_integration_tests
        run_api_tests
        ;;
    *)
        echo -e "${RED}Unknown test suite: ${TEST_SUITE}${NC}"
        echo "Available test suites: unit, integration, api, e2e, performance, security, contract, all"
        exit 1
        ;;
esac

# Generate test summary
echo -e "${YELLOW}Generating test summary...${NC}"
bash $(dirname "$0")/generate-test-report.sh

echo -e "${GREEN}Test execution completed!${NC}"