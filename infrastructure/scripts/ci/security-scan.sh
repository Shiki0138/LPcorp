#!/bin/bash
# Security scanning script for different scan types

set -euo pipefail

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Arguments
SCAN_TYPE="${1:-all}"
OUTPUT_DIR="security-results"

# Create output directory
mkdir -p ${OUTPUT_DIR}

echo -e "${YELLOW}Running security scan: ${SCAN_TYPE}${NC}"

# SAST Scan
run_sast_scan() {
    echo -e "${YELLOW}Running SAST scan...${NC}"
    
    # Semgrep scan
    if command -v semgrep &> /dev/null; then
        semgrep --config=auto \
            --json \
            --output=${OUTPUT_DIR}/semgrep-report.json \
            .
    fi
    
    # SonarQube scan
    if [ -n "${SONAR_HOST_URL:-}" ]; then
        sonar-scanner \
            -Dsonar.host.url=${SONAR_HOST_URL} \
            -Dsonar.login=${SONAR_TOKEN} \
            -Dsonar.projectKey=${CI_PROJECT_NAME} \
            -Dsonar.sources=. \
            -Dsonar.report.export.path=${OUTPUT_DIR}/sonar-report.json
    fi
    
    # GitLab SAST
    if [ -n "${CI_PROJECT_ID:-}" ]; then
        /analyzer run
        mv gl-sast-report.json ${OUTPUT_DIR}/
    fi
    
    # Bandit for Python
    if [ -f "requirements.txt" ]; then
        bandit -r . -f json -o ${OUTPUT_DIR}/bandit-report.json || true
    fi
    
    # ESLint security plugin for JavaScript
    if [ -f "package.json" ]; then
        npx eslint . --ext .js,.jsx,.ts,.tsx \
            --plugin security \
            --format json \
            --output-file ${OUTPUT_DIR}/eslint-security-report.json || true
    fi
}

# Dependency Scan
run_dependency_scan() {
    echo -e "${YELLOW}Running dependency scan...${NC}"
    
    # OWASP Dependency Check
    if command -v dependency-check &> /dev/null; then
        dependency-check \
            --project "${CI_PROJECT_NAME}" \
            --scan . \
            --format JSON \
            --out ${OUTPUT_DIR}/dependency-check-report.json \
            --enableExperimental
    fi
    
    # Snyk scan
    if command -v snyk &> /dev/null && [ -n "${SNYK_TOKEN:-}" ]; then
        snyk test --json > ${OUTPUT_DIR}/snyk-report.json || true
        snyk monitor || true
    fi
    
    # npm audit for Node.js
    if [ -f "package-lock.json" ]; then
        npm audit --json > ${OUTPUT_DIR}/npm-audit-report.json || true
    fi
    
    # pip-audit for Python
    if [ -f "requirements.txt" ]; then
        pip-audit --format json --output ${OUTPUT_DIR}/pip-audit-report.json || true
    fi
    
    # Safety for Python
    if [ -f "requirements.txt" ]; then
        safety check --json --output ${OUTPUT_DIR}/safety-report.json || true
    fi
    
    # bundler-audit for Ruby
    if [ -f "Gemfile.lock" ]; then
        bundle-audit check --format json > ${OUTPUT_DIR}/bundler-audit-report.json || true
    fi
}

# Container Scan
run_container_scan() {
    echo -e "${YELLOW}Running container scan...${NC}"
    
    # Trivy scan
    if command -v trivy &> /dev/null; then
        for image in $(docker images --format "{{.Repository}}:{{.Tag}}" | grep -v "<none>"); do
            echo "Scanning image: $image"
            trivy image \
                --format json \
                --output ${OUTPUT_DIR}/trivy-${image//[:\/]/-}.json \
                --severity HIGH,CRITICAL \
                $image || true
        done
    fi
    
    # Grype scan
    if command -v grype &> /dev/null; then
        for image in $(docker images --format "{{.Repository}}:{{.Tag}}" | grep -v "<none>"); do
            grype $image \
                --output json \
                --file ${OUTPUT_DIR}/grype-${image//[:\/]/-}.json || true
        done
    fi
    
    # Docker Scout
    if command -v docker-scout &> /dev/null; then
        docker scout cves --format json > ${OUTPUT_DIR}/docker-scout-report.json || true
    fi
    
    # Clair scan
    if [ -n "${CLAIR_URL:-}" ]; then
        clairctl analyze --format json > ${OUTPUT_DIR}/clair-report.json || true
    fi
}

# Infrastructure scan
run_infrastructure_scan() {
    echo -e "${YELLOW}Running infrastructure scan...${NC}"
    
    # Checkov scan
    if command -v checkov &> /dev/null; then
        checkov -d . \
            --framework all \
            --output json \
            --output-file-path ${OUTPUT_DIR}/checkov-report.json || true
    fi
    
    # tfsec for Terraform
    if [ -d "terraform" ] && command -v tfsec &> /dev/null; then
        tfsec terraform \
            --format json \
            --out ${OUTPUT_DIR}/tfsec-report.json || true
    fi
    
    # Terrascan
    if command -v terrascan &> /dev/null; then
        terrascan scan \
            --iac-type terraform \
            --output json \
            > ${OUTPUT_DIR}/terrascan-report.json || true
    fi
    
    # kube-score for Kubernetes
    if [ -d "k8s" ] && command -v kube-score &> /dev/null; then
        find k8s -name "*.yaml" -o -name "*.yml" | while read file; do
            kube-score score $file \
                --output-format json \
                > ${OUTPUT_DIR}/kube-score-$(basename $file).json || true
        done
    fi
}

# Secret scan
run_secret_scan() {
    echo -e "${YELLOW}Running secret scan...${NC}"
    
    # Trufflehog scan
    if command -v trufflehog &> /dev/null; then
        trufflehog filesystem . \
            --json \
            --no-verification \
            > ${OUTPUT_DIR}/trufflehog-report.json || true
    fi
    
    # Gitleaks scan
    if command -v gitleaks &> /dev/null; then
        gitleaks detect \
            --report-format json \
            --report-path ${OUTPUT_DIR}/gitleaks-report.json || true
    fi
    
    # detect-secrets scan
    if command -v detect-secrets &> /dev/null; then
        detect-secrets scan \
            --all-files \
            > ${OUTPUT_DIR}/detect-secrets-report.json || true
    fi
}

# License scan
run_license_scan() {
    echo -e "${YELLOW}Running license scan...${NC}"
    
    # License Finder
    if command -v license_finder &> /dev/null; then
        license_finder report --format json > ${OUTPUT_DIR}/license-finder-report.json || true
    fi
    
    # FOSSA scan
    if command -v fossa &> /dev/null && [ -n "${FOSSA_API_KEY:-}" ]; then
        fossa analyze
        fossa report licenses --json > ${OUTPUT_DIR}/fossa-report.json || true
    fi
    
    # Licensee
    if command -v licensee &> /dev/null; then
        licensee detect --json > ${OUTPUT_DIR}/licensee-report.json || true
    fi
}

# Compliance check
run_compliance_check() {
    echo -e "${YELLOW}Running compliance check...${NC}"
    
    # InSpec compliance
    if command -v inspec &> /dev/null; then
        inspec exec compliance/ \
            --reporter json:${OUTPUT_DIR}/inspec-report.json || true
    fi
    
    # Open Policy Agent
    if command -v opa &> /dev/null && [ -d "policies" ]; then
        opa test policies/ --format json > ${OUTPUT_DIR}/opa-report.json || true
    fi
}

# Generate consolidated report
generate_security_report() {
    echo -e "${YELLOW}Generating consolidated security report...${NC}"
    
    # Create summary report
    cat > ${OUTPUT_DIR}/security-summary.json <<EOF
{
    "scan_date": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
    "project": "${CI_PROJECT_NAME:-unknown}",
    "branch": "${CI_COMMIT_REF_NAME:-unknown}",
    "commit": "${CI_COMMIT_SHA:-unknown}",
    "scan_types": ["${SCAN_TYPE}"],
    "reports": []
}
EOF
    
    # Aggregate findings
    python3 - <<EOF
import json
import glob
import os

summary = {"total_findings": 0, "critical": 0, "high": 0, "medium": 0, "low": 0}
reports = []

for report_file in glob.glob("${OUTPUT_DIR}/*.json"):
    if "summary" in report_file:
        continue
    
    try:
        with open(report_file, 'r') as f:
            data = json.load(f)
            reports.append({
                "type": os.path.basename(report_file).replace('.json', ''),
                "file": report_file
            })
    except:
        pass

# Update summary
with open("${OUTPUT_DIR}/security-summary.json", 'r') as f:
    summary_data = json.load(f)

summary_data["reports"] = reports
summary_data["summary"] = summary

with open("${OUTPUT_DIR}/security-summary.json", 'w') as f:
    json.dump(summary_data, f, indent=2)
EOF
    
    # Generate HTML report
    if command -v pandoc &> /dev/null; then
        pandoc ${OUTPUT_DIR}/security-summary.json \
            -f json \
            -t html \
            -o ${OUTPUT_DIR}/security-report.html \
            --metadata title="Security Scan Report"
    fi
}

# Main execution
case $SCAN_TYPE in
    sast)
        run_sast_scan
        ;;
    dependency)
        run_dependency_scan
        ;;
    container)
        run_container_scan
        ;;
    infrastructure)
        run_infrastructure_scan
        ;;
    secret)
        run_secret_scan
        ;;
    license)
        run_license_scan
        ;;
    compliance)
        run_compliance_check
        ;;
    all)
        run_sast_scan
        run_dependency_scan
        run_container_scan
        run_infrastructure_scan
        run_secret_scan
        run_license_scan
        run_compliance_check
        ;;
    *)
        echo -e "${RED}Unknown scan type: ${SCAN_TYPE}${NC}"
        echo "Available scan types: sast, dependency, container, infrastructure, secret, license, compliance, all"
        exit 1
        ;;
esac

# Generate report
generate_security_report

# Check for critical findings
CRITICAL_COUNT=$(jq -r '.summary.critical // 0' ${OUTPUT_DIR}/security-summary.json)
if [ "$CRITICAL_COUNT" -gt 0 ]; then
    echo -e "${RED}Found $CRITICAL_COUNT critical security issues!${NC}"
    exit 1
fi

echo -e "${GREEN}Security scan completed successfully!${NC}"