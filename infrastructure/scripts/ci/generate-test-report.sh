#!/bin/bash
# Generate consolidated test report from various test results

set -euo pipefail

# Color codes
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Output directory
REPORT_DIR="test-summary"
mkdir -p ${REPORT_DIR}

echo -e "${YELLOW}Generating test report...${NC}"

# Function to parse JUnit XML reports
parse_junit_reports() {
    local junit_files=$(find . -name "*.xml" -path "*/target/surefire-reports/*" -o -path "*/test-results/*" -o -path "*/junit/*" 2>/dev/null)
    
    if [ -z "$junit_files" ]; then
        echo "No JUnit reports found"
        return
    fi
    
    # Create summary
    cat > ${REPORT_DIR}/junit-summary.json <<EOF
{
    "total_tests": 0,
    "passed": 0,
    "failed": 0,
    "skipped": 0,
    "errors": 0,
    "duration": 0,
    "test_suites": []
}
EOF
    
    # Parse each JUnit file
    for file in $junit_files; do
        if [ -f "$file" ]; then
            # Extract test statistics using xmlstarlet or python
            python3 - "$file" <<'PYTHON_SCRIPT'
import sys
import xml.etree.ElementTree as ET
import json

try:
    tree = ET.parse(sys.argv[1])
    root = tree.getroot()
    
    # Handle both testsuite and testsuites root elements
    if root.tag == 'testsuites':
        testsuites = root.findall('testsuite')
    else:
        testsuites = [root]
    
    for testsuite in testsuites:
        tests = int(testsuite.get('tests', 0))
        failures = int(testsuite.get('failures', 0))
        errors = int(testsuite.get('errors', 0))
        skipped = int(testsuite.get('skipped', 0))
        time = float(testsuite.get('time', 0))
        
        # Update summary
        with open('test-summary/junit-summary.json', 'r') as f:
            summary = json.load(f)
        
        summary['total_tests'] += tests
        summary['failed'] += failures
        summary['errors'] += errors
        summary['skipped'] += skipped
        summary['passed'] += (tests - failures - errors - skipped)
        summary['duration'] += time
        
        summary['test_suites'].append({
            'name': testsuite.get('name', 'Unknown'),
            'tests': tests,
            'failures': failures,
            'errors': errors,
            'skipped': skipped,
            'time': time
        })
        
        with open('test-summary/junit-summary.json', 'w') as f:
            json.dump(summary, f, indent=2)
            
except Exception as e:
    print(f"Error parsing {sys.argv[1]}: {e}")
PYTHON_SCRIPT
        fi
    done
}

# Function to parse coverage reports
parse_coverage_reports() {
    echo -e "${YELLOW}Parsing coverage reports...${NC}"
    
    # Initialize coverage summary
    cat > ${REPORT_DIR}/coverage-summary.json <<EOF
{
    "overall_coverage": 0,
    "line_coverage": 0,
    "branch_coverage": 0,
    "services": []
}
EOF
    
    # Find and parse Jacoco reports
    local jacoco_files=$(find . -name "jacoco.xml" -o -name "jacoco.csv" 2>/dev/null)
    for file in $jacoco_files; do
        if [[ $file == *.xml ]]; then
            # Parse XML coverage
            coverage=$(grep -o 'coverage="[0-9.]*"' "$file" | head -1 | grep -o '[0-9.]*' || echo "0")
        elif [[ $file == *.csv ]]; then
            # Parse CSV coverage
            coverage=$(awk -F"," '{ instructions += $4 + $5; covered += $5 } END { print 100*covered/instructions }' "$file" || echo "0")
        fi
        
        service_name=$(echo "$file" | awk -F'/' '{print $(NF-3)}')
        
        # Update summary
        python3 - <<PYTHON_SCRIPT
import json
with open('${REPORT_DIR}/coverage-summary.json', 'r') as f:
    summary = json.load(f)
summary['services'].append({
    'name': '${service_name}',
    'coverage': ${coverage:-0}
})
# Recalculate overall coverage
if summary['services']:
    summary['overall_coverage'] = sum(s['coverage'] for s in summary['services']) / len(summary['services'])
with open('${REPORT_DIR}/coverage-summary.json', 'w') as f:
    json.dump(summary, f, indent=2)
PYTHON_SCRIPT
    done
    
    # Parse LCOV reports for JavaScript
    local lcov_files=$(find . -name "lcov.info" -o -name "coverage-final.json" 2>/dev/null)
    for file in $lcov_files; do
        if [[ $file == *.info ]]; then
            # Parse LCOV
            lines_hit=$(grep -c '^DA:[0-9]*,1' "$file" || echo "0")
            lines_total=$(grep -c '^DA:' "$file" || echo "1")
            coverage=$(awk "BEGIN {printf \"%.2f\", 100*${lines_hit}/${lines_total}}")
        fi
    done
}

# Function to generate HTML report
generate_html_report() {
    echo -e "${YELLOW}Generating HTML report...${NC}"
    
    cat > ${REPORT_DIR}/test-report.html <<'HTML'
<!DOCTYPE html>
<html>
<head>
    <title>Test Execution Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background-color: #f0f0f0; padding: 20px; border-radius: 5px; }
        .summary { display: flex; justify-content: space-around; margin: 20px 0; }
        .metric { text-align: center; padding: 20px; background: #fff; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .metric h3 { margin: 0; color: #333; }
        .metric .value { font-size: 2em; font-weight: bold; margin: 10px 0; }
        .passed { color: #28a745; }
        .failed { color: #dc3545; }
        .skipped { color: #ffc107; }
        .coverage { color: #17a2b8; }
        table { width: 100%; border-collapse: collapse; margin: 20px 0; }
        th, td { padding: 10px; text-align: left; border-bottom: 1px solid #ddd; }
        th { background-color: #f8f9fa; }
        .chart { width: 100%; height: 300px; margin: 20px 0; }
    </style>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</head>
<body>
    <div class="header">
        <h1>Test Execution Report</h1>
        <p>Generated on: <span id="timestamp"></span></p>
        <p>Pipeline: ${CI_PIPELINE_ID:-Local}</p>
        <p>Commit: ${CI_COMMIT_SHORT_SHA:-Unknown}</p>
    </div>
    
    <div class="summary">
        <div class="metric">
            <h3>Total Tests</h3>
            <div class="value" id="total-tests">0</div>
        </div>
        <div class="metric">
            <h3>Passed</h3>
            <div class="value passed" id="passed-tests">0</div>
        </div>
        <div class="metric">
            <h3>Failed</h3>
            <div class="value failed" id="failed-tests">0</div>
        </div>
        <div class="metric">
            <h3>Skipped</h3>
            <div class="value skipped" id="skipped-tests">0</div>
        </div>
        <div class="metric">
            <h3>Coverage</h3>
            <div class="value coverage" id="coverage">0%</div>
        </div>
    </div>
    
    <h2>Test Results by Service</h2>
    <table id="service-results">
        <thead>
            <tr>
                <th>Service</th>
                <th>Tests</th>
                <th>Passed</th>
                <th>Failed</th>
                <th>Coverage</th>
                <th>Duration</th>
            </tr>
        </thead>
        <tbody></tbody>
    </table>
    
    <h2>Test Execution Trend</h2>
    <canvas id="trend-chart" class="chart"></canvas>
    
    <h2>Coverage Distribution</h2>
    <canvas id="coverage-chart" class="chart"></canvas>
    
    <script>
        // Load test data
        fetch('junit-summary.json')
            .then(response => response.json())
            .then(data => {
                document.getElementById('total-tests').textContent = data.total_tests;
                document.getElementById('passed-tests').textContent = data.passed;
                document.getElementById('failed-tests').textContent = data.failed;
                document.getElementById('skipped-tests').textContent = data.skipped;
                
                // Populate service results table
                const tbody = document.querySelector('#service-results tbody');
                data.test_suites.forEach(suite => {
                    const row = tbody.insertRow();
                    row.innerHTML = \`
                        <td>\${suite.name}</td>
                        <td>\${suite.tests}</td>
                        <td class="passed">\${suite.tests - suite.failures - suite.errors - suite.skipped}</td>
                        <td class="failed">\${suite.failures + suite.errors}</td>
                        <td>-</td>
                        <td>\${suite.time.toFixed(2)}s</td>
                    \`;
                });
            });
        
        // Load coverage data
        fetch('coverage-summary.json')
            .then(response => response.json())
            .then(data => {
                document.getElementById('coverage').textContent = data.overall_coverage.toFixed(1) + '%';
            });
        
        // Set timestamp
        document.getElementById('timestamp').textContent = new Date().toLocaleString();
    </script>
</body>
</html>
HTML
}

# Function to create aggregated JUnit report
create_aggregated_junit() {
    echo -e "${YELLOW}Creating aggregated JUnit report...${NC}"
    
    cat > ${REPORT_DIR}/aggregated-test-results.xml <<XML
<?xml version="1.0" encoding="UTF-8"?>
<testsuites>
XML
    
    # Aggregate all JUnit reports
    find . -name "*.xml" -path "*/target/surefire-reports/*" -o -path "*/test-results/*" 2>/dev/null | while read file; do
        if [ -f "$file" ]; then
            # Extract testsuite content and append
            sed -n '/<testsuite/,/<\/testsuite>/p' "$file" >> ${REPORT_DIR}/aggregated-test-results.xml
        fi
    done
    
    echo "</testsuites>" >> ${REPORT_DIR}/aggregated-test-results.xml
}

# Main execution
echo -e "${YELLOW}Collecting test results...${NC}"

# Parse different report types
parse_junit_reports
parse_coverage_reports
generate_html_report
create_aggregated_junit

# Create final summary
cat > ${REPORT_DIR}/summary.json <<EOF
{
    "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
    "pipeline_id": "${CI_PIPELINE_ID:-local}",
    "commit": "${CI_COMMIT_SHA:-unknown}",
    "branch": "${CI_COMMIT_REF_NAME:-unknown}",
    "test_results": $(cat ${REPORT_DIR}/junit-summary.json 2>/dev/null || echo '{}'),
    "coverage": $(cat ${REPORT_DIR}/coverage-summary.json 2>/dev/null || echo '{}')
}
EOF

echo -e "${GREEN}Test report generated in ${REPORT_DIR}/${NC}"

# Display summary
if [ -f "${REPORT_DIR}/junit-summary.json" ]; then
    total=$(jq -r '.total_tests' ${REPORT_DIR}/junit-summary.json)
    passed=$(jq -r '.passed' ${REPORT_DIR}/junit-summary.json)
    failed=$(jq -r '.failed' ${REPORT_DIR}/junit-summary.json)
    
    echo -e "${GREEN}Test Summary:${NC}"
    echo -e "Total Tests: ${total}"
    echo -e "Passed: ${GREEN}${passed}${NC}"
    echo -e "Failed: ${failed}"
    
    if [ "$failed" -gt 0 ]; then
        exit 1
    fi
fi