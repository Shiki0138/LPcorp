#!/usr/bin/env node

/**
 * Real-time Monitoring & Alerting System
 * Enterprise-grade quality monitoring
 */

const fs = require('fs');
const path = require('path');
const { EventEmitter } = require('events');

// Monitoring configuration
const MONITORING_CONFIG = {
  metrics: {
    errorRate: { threshold: 0.01, unit: '%' },
    responseTime: { threshold: 500, unit: 'ms' },
    throughput: { threshold: 100, unit: 'rps' },
    availability: { threshold: 99.9, unit: '%' },
    cpuUsage: { threshold: 80, unit: '%' },
    memoryUsage: { threshold: 85, unit: '%' },
    testCoverage: { threshold: 95, unit: '%' },
    codeQuality: { threshold: 90, unit: 'score' }
  },
  alerts: {
    critical: {
      channels: ['email', 'slack', 'pagerduty'],
      escalation: 'immediate'
    },
    warning: {
      channels: ['slack'],
      escalation: '5min'
    },
    info: {
      channels: ['log'],
      escalation: 'none'
    }
  },
  intervals: {
    healthCheck: 30000,    // 30 seconds
    metricsCollection: 60000,  // 1 minute
    reportGeneration: 300000,  // 5 minutes
    dashboardUpdate: 10000     // 10 seconds
  }
};

class QAMonitoringSystem extends EventEmitter {
  constructor() {
    super();
    this.metrics = new Map();
    this.alerts = [];
    this.isRunning = false;
    this.intervals = new Map();
    this.dashboardData = {
      timestamp: new Date().toISOString(),
      status: 'initializing',
      metrics: {},
      alerts: [],
      trends: {}
    };
  }

  async start() {
    console.log('🚀 Starting QA Monitoring System...');
    
    this.isRunning = true;
    
    // Initialize metrics storage
    this.initializeMetrics();
    
    // Setup monitoring intervals
    this.setupIntervals();
    
    // Setup event listeners
    this.setupEventListeners();
    
    // Start dashboard server
    await this.startDashboard();
    
    console.log('✅ QA Monitoring System started successfully');
    console.log('📊 Dashboard available at: http://localhost:3001/qa-dashboard');
  }

  async stop() {
    console.log('🛑 Stopping QA Monitoring System...');
    
    this.isRunning = false;
    
    // Clear intervals
    this.intervals.forEach(interval => clearInterval(interval));
    this.intervals.clear();
    
    // Save final metrics
    this.saveMetrics();
    
    console.log('✅ QA Monitoring System stopped');
  }

  initializeMetrics() {
    Object.keys(MONITORING_CONFIG.metrics).forEach(metric => {
      this.metrics.set(metric, {
        current: 0,
        threshold: MONITORING_CONFIG.metrics[metric].threshold,
        unit: MONITORING_CONFIG.metrics[metric].unit,
        history: [],
        trend: 'stable',
        lastAlert: null
      });
    });
  }

  setupIntervals() {
    // Health check interval
    this.intervals.set('healthCheck', setInterval(() => {
      this.performHealthCheck();
    }, MONITORING_CONFIG.intervals.healthCheck));

    // Metrics collection interval
    this.intervals.set('metricsCollection', setInterval(() => {
      this.collectMetrics();
    }, MONITORING_CONFIG.intervals.metricsCollection));

    // Report generation interval
    this.intervals.set('reportGeneration', setInterval(() => {
      this.generateReport();
    }, MONITORING_CONFIG.intervals.reportGeneration));

    // Dashboard update interval
    this.intervals.set('dashboardUpdate', setInterval(() => {
      this.updateDashboard();
    }, MONITORING_CONFIG.intervals.dashboardUpdate));
  }

  setupEventListeners() {
    // Listen for metric threshold violations
    this.on('metricViolation', (data) => {
      this.handleMetricViolation(data);
    });

    // Listen for system events
    this.on('systemEvent', (data) => {
      this.handleSystemEvent(data);
    });

    // Listen for test completion
    this.on('testCompleted', (data) => {
      this.handleTestCompletion(data);
    });

    // Process exit handlers
    process.on('SIGINT', () => this.stop());
    process.on('SIGTERM', () => this.stop());
  }

  async performHealthCheck() {
    if (!this.isRunning) return;

    const healthStatus = {
      timestamp: new Date().toISOString(),
      status: 'healthy',
      checks: {}
    };

    try {
      // Check system resources
      const memoryUsage = process.memoryUsage();
      const memoryUsagePercent = (memoryUsage.heapUsed / memoryUsage.heapTotal) * 100;
      
      healthStatus.checks.memory = {
        status: memoryUsagePercent < 85 ? 'healthy' : 'warning',
        value: memoryUsagePercent,
        unit: '%'
      };

      // Check file system
      const coverageExists = fs.existsSync('coverage');
      healthStatus.checks.fileSystem = {
        status: coverageExists ? 'healthy' : 'warning',
        value: coverageExists ? 'accessible' : 'coverage directory missing'
      };

      // Check test results
      const testResults = this.getLatestTestResults();
      healthStatus.checks.tests = {
        status: testResults.passed ? 'healthy' : 'critical',
        value: testResults.passRate,
        unit: '%'
      };

      // Update overall status
      const allChecks = Object.values(healthStatus.checks);
      const criticalCount = allChecks.filter(check => check.status === 'critical').length;
      const warningCount = allChecks.filter(check => check.status === 'warning').length;

      if (criticalCount > 0) {
        healthStatus.status = 'critical';
      } else if (warningCount > 0) {
        healthStatus.status = 'warning';
      }

      // Emit events for status changes
      if (healthStatus.status !== this.dashboardData.status) {
        this.emit('systemEvent', {
          type: 'statusChange',
          from: this.dashboardData.status,
          to: healthStatus.status,
          timestamp: new Date().toISOString()
        });
      }

      this.dashboardData.status = healthStatus.status;

    } catch (error) {
      console.error('❌ Health check failed:', error.message);
      healthStatus.status = 'critical';
      healthStatus.error = error.message;
    }
  }

  collectMetrics() {
    if (!this.isRunning) return;

    try {
      // Collect system metrics
      const systemMetrics = this.collectSystemMetrics();
      
      // Collect test metrics
      const testMetrics = this.collectTestMetrics();
      
      // Collect performance metrics
      const performanceMetrics = this.collectPerformanceMetrics();
      
      // Update metrics
      Object.entries({ ...systemMetrics, ...testMetrics, ...performanceMetrics }).forEach(([key, value]) => {
        this.updateMetric(key, value);
      });

    } catch (error) {
      console.error('❌ Metrics collection failed:', error.message);
    }
  }

  collectSystemMetrics() {
    const memoryUsage = process.memoryUsage();
    const memoryUsagePercent = (memoryUsage.heapUsed / memoryUsage.heapTotal) * 100;
    
    // Mock CPU usage (would use actual CPU monitoring in production)
    const cpuUsage = Math.random() * 20 + 40; // 40-60% range
    
    return {
      memoryUsage: memoryUsagePercent,
      cpuUsage
    };
  }

  collectTestMetrics() {
    const testResults = this.getLatestTestResults();
    const coverageData = this.getLatestCoverage();
    
    return {
      testCoverage: coverageData.statements || 0,
      codeQuality: testResults.qualityScore || 90
    };
  }

  collectPerformanceMetrics() {
    // Mock performance metrics (would integrate with APM tools in production)
    return {
      responseTime: Math.random() * 200 + 100, // 100-300ms
      throughput: Math.random() * 500 + 800,   // 800-1300 rps
      errorRate: Math.random() * 0.02,         // 0-0.02%
      availability: 99.8 + Math.random() * 0.2 // 99.8-100%
    };
  }

  updateMetric(metricName, value) {
    const metric = this.metrics.get(metricName);
    if (!metric) return;

    // Update current value
    const previousValue = metric.current;
    metric.current = value;

    // Add to history
    metric.history.push({
      value,
      timestamp: new Date().toISOString()
    });

    // Keep only last 100 entries
    if (metric.history.length > 100) {
      metric.history = metric.history.slice(-100);
    }

    // Calculate trend
    if (metric.history.length >= 5) {
      const recent = metric.history.slice(-5).map(h => h.value);
      const average = recent.reduce((sum, val) => sum + val, 0) / recent.length;
      
      if (value > average * 1.1) {
        metric.trend = 'increasing';
      } else if (value < average * 0.9) {
        metric.trend = 'decreasing';
      } else {
        metric.trend = 'stable';
      }
    }

    // Check for threshold violations
    if (this.isThresholdViolation(metricName, value)) {
      this.emit('metricViolation', {
        metric: metricName,
        value,
        threshold: metric.threshold,
        unit: metric.unit,
        previousValue,
        timestamp: new Date().toISOString()
      });
    }
  }

  isThresholdViolation(metricName, value) {
    const metric = this.metrics.get(metricName);
    if (!metric) return false;

    // Different threshold logic for different metrics
    switch (metricName) {
      case 'errorRate':
      case 'cpuUsage':
      case 'memoryUsage':
        return value > metric.threshold;
      case 'responseTime':
        return value > metric.threshold;
      case 'availability':
      case 'testCoverage':
      case 'codeQuality':
        return value < metric.threshold;
      case 'throughput':
        return value < metric.threshold;
      default:
        return false;
    }
  }

  handleMetricViolation(data) {
    const alert = {
      id: `alert_${Date.now()}`,
      type: 'metricViolation',
      severity: this.calculateAlertSeverity(data),
      metric: data.metric,
      value: data.value,
      threshold: data.threshold,
      message: this.generateAlertMessage(data),
      timestamp: new Date().toISOString(),
      resolved: false
    };

    this.alerts.push(alert);
    
    // Send alert through configured channels
    this.sendAlert(alert);
    
    console.warn(`⚠️  Metric violation: ${alert.message}`);
  }

  calculateAlertSeverity(data) {
    const deviation = Math.abs((data.value - data.threshold) / data.threshold);
    
    if (deviation > 0.5) return 'critical';
    if (deviation > 0.2) return 'warning';
    return 'info';
  }

  generateAlertMessage(data) {
    return `${data.metric} is ${data.value}${data.unit}, exceeding threshold of ${data.threshold}${data.unit}`;
  }

  sendAlert(alert) {
    const config = MONITORING_CONFIG.alerts[alert.severity];
    
    config.channels.forEach(channel => {
      switch (channel) {
        case 'email':
          this.sendEmailAlert(alert);
          break;
        case 'slack':
          this.sendSlackAlert(alert);
          break;
        case 'pagerduty':
          this.sendPagerDutyAlert(alert);
          break;
        case 'log':
          this.logAlert(alert);
          break;
      }
    });
  }

  sendEmailAlert(alert) {
    // Mock email alert
    console.log(`📧 Email alert sent: ${alert.message}`);
  }

  sendSlackAlert(alert) {
    // Mock Slack alert
    console.log(`💬 Slack alert sent: ${alert.message}`);
  }

  sendPagerDutyAlert(alert) {
    // Mock PagerDuty alert
    console.log(`📟 PagerDuty alert sent: ${alert.message}`);
  }

  logAlert(alert) {
    const logEntry = {
      timestamp: alert.timestamp,
      level: alert.severity.toUpperCase(),
      type: 'ALERT',
      message: alert.message,
      metadata: {
        metric: alert.metric,
        value: alert.value,
        threshold: alert.threshold
      }
    };
    
    console.log(`📝 ${JSON.stringify(logEntry)}`);
  }

  handleSystemEvent(data) {
    console.log(`🔔 System event: ${data.type} - ${JSON.stringify(data)}`);
  }

  handleTestCompletion(data) {
    console.log(`✅ Test completed: ${data.suite} - ${data.passed ? 'PASSED' : 'FAILED'}`);
    
    // Update test metrics
    this.updateMetric('testCoverage', data.coverage || 0);
    this.updateMetric('codeQuality', data.qualityScore || 90);
  }

  getLatestTestResults() {
    // Mock test results (would read from actual test output in production)
    return {
      passed: true,
      total: 150,
      failed: 0,
      passRate: 100,
      qualityScore: 94
    };
  }

  getLatestCoverage() {
    // Try to read coverage data
    try {
      if (fs.existsSync('coverage/coverage-summary.json')) {
        const coverageData = JSON.parse(fs.readFileSync('coverage/coverage-summary.json', 'utf8'));
        return coverageData.total;
      }
    } catch (error) {
      console.warn('Could not read coverage data:', error.message);
    }
    
    // Mock coverage data
    return {
      statements: 95.2,
      branches: 91.8,
      functions: 96.1,
      lines: 94.9
    };
  }

  generateReport() {
    if (!this.isRunning) return;

    const report = {
      timestamp: new Date().toISOString(),
      period: '5min',
      summary: {
        status: this.dashboardData.status,
        totalAlerts: this.alerts.length,
        activeAlerts: this.alerts.filter(a => !a.resolved).length,
        systemHealth: this.calculateSystemHealth()
      },
      metrics: this.getMetricsSummary(),
      alerts: this.alerts.slice(-10), // Last 10 alerts
      trends: this.calculateTrends()
    };

    // Save report
    const reportPath = `coverage/monitoring-report-${Date.now()}.json`;
    fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
    
    console.log(`📊 Monitoring report generated: ${reportPath}`);
  }

  calculateSystemHealth() {
    const metrics = Array.from(this.metrics.values());
    const healthyMetrics = metrics.filter(m => !this.isThresholdViolation(m.name, m.current));
    
    return Math.round((healthyMetrics.length / metrics.length) * 100);
  }

  getMetricsSummary() {
    const summary = {};
    
    this.metrics.forEach((data, name) => {
      summary[name] = {
        current: data.current,
        threshold: data.threshold,
        unit: data.unit,
        trend: data.trend,
        status: this.isThresholdViolation(name, data.current) ? 'violation' : 'normal'
      };
    });
    
    return summary;
  }

  calculateTrends() {
    const trends = {};
    
    this.metrics.forEach((data, name) => {
      if (data.history.length >= 10) {
        const recent10 = data.history.slice(-10).map(h => h.value);
        const avg = recent10.reduce((sum, val) => sum + val, 0) / recent10.length;
        const slope = this.calculateSlope(recent10);
        
        trends[name] = {
          direction: slope > 0 ? 'increasing' : slope < 0 ? 'decreasing' : 'stable',
          rate: Math.abs(slope),
          average: avg
        };
      }
    });
    
    return trends;
  }

  calculateSlope(values) {
    const n = values.length;
    const sumX = (n * (n - 1)) / 2;
    const sumY = values.reduce((sum, val) => sum + val, 0);
    const sumXY = values.reduce((sum, val, i) => sum + val * i, 0);
    const sumX2 = values.reduce((sum, val, i) => sum + i * i, 0);
    
    return (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
  }

  updateDashboard() {
    this.dashboardData = {
      timestamp: new Date().toISOString(),
      status: this.dashboardData.status,
      metrics: this.getMetricsSummary(),
      alerts: this.alerts.filter(a => !a.resolved).slice(-5),
      trends: this.calculateTrends(),
      systemHealth: this.calculateSystemHealth()
    };
  }

  async startDashboard() {
    // Simple HTTP server for dashboard (would use proper framework in production)
    const http = require('http');
    
    const server = http.createServer((req, res) => {
      if (req.url === '/qa-dashboard') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(this.dashboardData, null, 2));
      } else {
        res.writeHead(404, { 'Content-Type': 'text/plain' });
        res.end('Not Found');
      }
    });
    
    server.listen(3001, () => {
      console.log('📊 Dashboard server started on port 3001');
    });
  }

  saveMetrics() {
    const metricsData = {
      timestamp: new Date().toISOString(),
      metrics: Object.fromEntries(this.metrics),
      alerts: this.alerts
    };
    
    const metricsPath = 'coverage/metrics-backup.json';
    fs.writeFileSync(metricsPath, JSON.stringify(metricsData, null, 2));
    
    console.log(`💾 Metrics saved to: ${metricsPath}`);
  }
}

// CLI interface
if (require.main === module) {
  const monitoring = new QAMonitoringSystem();
  
  monitoring.start().catch(error => {
    console.error('💥 Failed to start monitoring system:', error);
    process.exit(1);
  });
}

module.exports = QAMonitoringSystem;