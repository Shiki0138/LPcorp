#!/usr/bin/env node

/**
 * Quality Metrics Dashboard & KPI Tracking
 * Enterprise-grade quality metrics visualization and tracking
 */

const fs = require('fs');
const path = require('path');
const http = require('http');
const url = require('url');

// Dashboard configuration
const DASHBOARD_CONFIG = {
  port: 3002,
  refreshInterval: 5000, // 5 seconds
  dataRetention: {
    metrics: 1000,     // Keep last 1000 metric points
    alerts: 100,       // Keep last 100 alerts
    reviews: 50        // Keep last 50 reviews
  },
  kpis: {
    bugEscapeRate: { target: 0.01, unit: '%', critical: 0.05 },
    testCoverage: { target: 95, unit: '%', critical: 85 },
    performanceScore: { target: 95, unit: 'score', critical: 80 },
    securityScore: { target: 95, unit: 'score', critical: 75 },
    codeQuality: { target: 90, unit: 'score', critical: 70 },
    deploymentSuccess: { target: 99, unit: '%', critical: 95 },
    mttr: { target: 15, unit: 'minutes', critical: 60 },
    reviewApprovalRate: { target: 90, unit: '%', critical: 70 }
  },
  colors: {
    excellent: '#00C851',
    good: '#00C851',
    warning: '#ffbb33',
    critical: '#ff4444',
    unknown: '#666666'
  }
};

class QualityMetricsDashboard {
  constructor() {
    this.metrics = new Map();
    this.kpiHistory = new Map();
    this.alerts = [];
    this.reviews = [];
    this.trends = new Map();
    this.server = null;
    this.isRunning = false;
  }

  async start() {
    console.log('🚀 Starting Quality Metrics Dashboard...');
    
    // Initialize data
    this.initializeData();
    
    // Load historical data
    await this.loadHistoricalData();
    
    // Start data collection
    this.startDataCollection();
    
    // Start web server
    await this.startWebServer();
    
    this.isRunning = true;
    
    console.log('✅ Quality Metrics Dashboard started successfully');
    console.log(`📊 Dashboard available at: http://localhost:${DASHBOARD_CONFIG.port}/dashboard`);
  }

  async stop() {
    console.log('🛑 Stopping Quality Metrics Dashboard...');
    
    this.isRunning = false;
    
    if (this.server) {
      this.server.close();
    }
    
    // Save current state
    await this.saveCurrentState();
    
    console.log('✅ Dashboard stopped');
  }

  initializeData() {
    // Initialize KPI tracking
    Object.keys(DASHBOARD_CONFIG.kpis).forEach(kpi => {
      this.kpiHistory.set(kpi, []);
      this.trends.set(kpi, { direction: 'stable', confidence: 0 });
    });
    
    // Initialize with mock data for demonstration
    this.generateMockData();
  }

  generateMockData() {
    const now = Date.now();
    const oneHour = 60 * 60 * 1000;
    
    // Generate historical data points (last 24 hours)
    for (let i = 24; i >= 0; i--) {
      const timestamp = now - (i * oneHour);
      
      Object.keys(DASHBOARD_CONFIG.kpis).forEach(kpi => {
        const history = this.kpiHistory.get(kpi);
        const target = DASHBOARD_CONFIG.kpis[kpi].target;
        
        // Generate realistic mock values around targets
        let value;
        switch (kpi) {
          case 'bugEscapeRate':
            value = Math.random() * 0.02;
            break;
          case 'testCoverage':
            value = 92 + Math.random() * 6;
            break;
          case 'performanceScore':
            value = 90 + Math.random() * 8;
            break;
          case 'securityScore':
            value = 88 + Math.random() * 10;
            break;
          case 'codeQuality':
            value = 87 + Math.random() * 10;
            break;
          case 'deploymentSuccess':
            value = 96 + Math.random() * 4;
            break;
          case 'mttr':
            value = 10 + Math.random() * 20;
            break;
          case 'reviewApprovalRate':
            value = 85 + Math.random() * 12;
            break;
          default:
            value = target * (0.9 + Math.random() * 0.2);
        }
        
        history.push({
          timestamp,
          value: Math.round(value * 100) / 100,
          status: this.calculateStatus(kpi, value)
        });
      });
    }
    
    // Calculate trends
    this.calculateTrends();
  }

  async loadHistoricalData() {
    try {
      const dataPath = 'coverage/dashboard-data.json';
      if (fs.existsSync(dataPath)) {
        const data = JSON.parse(fs.readFileSync(dataPath, 'utf8'));
        
        if (data.kpiHistory) {
          this.kpiHistory = new Map(Object.entries(data.kpiHistory));
        }
        
        if (data.alerts) {
          this.alerts = data.alerts;
        }
        
        if (data.reviews) {
          this.reviews = data.reviews;
        }
        
        console.log('📊 Historical data loaded');
      }
    } catch (error) {
      console.warn('⚠️  Could not load historical data:', error.message);
    }
  }

  startDataCollection() {
    // Simulate real-time data collection
    setInterval(() => {
      this.collectCurrentMetrics();
    }, DASHBOARD_CONFIG.refreshInterval);
  }

  collectCurrentMetrics() {
    if (!this.isRunning) return;
    
    const timestamp = Date.now();
    
    // Collect real metrics (in production, this would interface with actual systems)
    const currentMetrics = this.getCurrentMetrics();
    
    // Update KPI history
    Object.entries(currentMetrics).forEach(([kpi, value]) => {
      const history = this.kpiHistory.get(kpi) || [];
      
      history.push({
        timestamp,
        value,
        status: this.calculateStatus(kpi, value)
      });
      
      // Maintain data retention limits
      if (history.length > DASHBOARD_CONFIG.dataRetention.metrics) {
        history.splice(0, history.length - DASHBOARD_CONFIG.dataRetention.metrics);
      }
      
      this.kpiHistory.set(kpi, history);
    });
    
    // Check for alerts
    this.checkForAlerts(currentMetrics, timestamp);
    
    // Update trends
    this.calculateTrends();
  }

  getCurrentMetrics() {
    // In production, this would collect from various sources:
    // - Test results
    // - Coverage reports
    // - Performance monitoring
    // - Security scans
    // - Deployment logs
    // - Review system
    
    // For now, simulate with slight variations
    const metrics = {};
    
    Object.keys(DASHBOARD_CONFIG.kpis).forEach(kpi => {
      const history = this.kpiHistory.get(kpi) || [];
      const lastValue = history.length > 0 ? history[history.length - 1].value : DASHBOARD_CONFIG.kpis[kpi].target;
      
      // Add small random variation
      const variation = (Math.random() - 0.5) * 2; // -1 to +1
      const newValue = Math.max(0, lastValue + variation);
      
      metrics[kpi] = Math.round(newValue * 100) / 100;
    });
    
    return metrics;
  }

  calculateStatus(kpi, value) {
    const config = DASHBOARD_CONFIG.kpis[kpi];
    if (!config) return 'unknown';
    
    const target = config.target;
    const critical = config.critical;
    
    // Different logic for different metric types
    if (kpi === 'bugEscapeRate' || kpi === 'mttr') {
      // Lower is better
      if (value <= target) return 'excellent';
      if (value <= critical) return 'good';
      if (value <= critical * 1.5) return 'warning';
      return 'critical';
    } else {
      // Higher is better
      if (value >= target) return 'excellent';
      if (value >= critical) return 'good';
      if (value >= critical * 0.9) return 'warning';
      return 'critical';
    }
  }

  checkForAlerts(currentMetrics, timestamp) {
    Object.entries(currentMetrics).forEach(([kpi, value]) => {
      const status = this.calculateStatus(kpi, value);
      
      if (status === 'critical' || status === 'warning') {
        // Check if we already have a recent alert for this KPI
        const recentAlert = this.alerts.find(alert => 
          alert.kpi === kpi && 
          alert.timestamp > timestamp - (5 * 60 * 1000) && // Within 5 minutes
          !alert.resolved
        );
        
        if (!recentAlert) {
          const alert = {
            id: `alert_${timestamp}_${kpi}`,
            kpi,
            value,
            status,
            severity: status === 'critical' ? 'high' : 'medium',
            message: this.generateAlertMessage(kpi, value, status),
            timestamp,
            resolved: false
          };
          
          this.alerts.push(alert);
          
          // Maintain alert retention
          if (this.alerts.length > DASHBOARD_CONFIG.dataRetention.alerts) {
            this.alerts.splice(0, this.alerts.length - DASHBOARD_CONFIG.dataRetention.alerts);
          }
          
          console.warn(`🚨 Alert: ${alert.message}`);
        }
      }
    });
  }

  generateAlertMessage(kpi, value, status) {
    const config = DASHBOARD_CONFIG.kpis[kpi];
    const kpiName = kpi.replace(/([A-Z])/g, ' $1').toLowerCase();
    
    return `${kpiName} is ${status}: ${value}${config.unit} (target: ${config.target}${config.unit})`;
  }

  calculateTrends() {
    Object.keys(DASHBOARD_CONFIG.kpis).forEach(kpi => {
      const history = this.kpiHistory.get(kpi) || [];
      
      if (history.length < 5) {
        this.trends.set(kpi, { direction: 'stable', confidence: 0 });
        return;
      }
      
      const recent = history.slice(-5).map(point => point.value);
      const older = history.slice(-10, -5).map(point => point.value);
      
      if (older.length === 0) {
        this.trends.set(kpi, { direction: 'stable', confidence: 0 });
        return;
      }
      
      const recentAvg = recent.reduce((sum, val) => sum + val, 0) / recent.length;
      const olderAvg = older.reduce((sum, val) => sum + val, 0) / older.length;
      
      const change = recentAvg - olderAvg;
      const changePercent = Math.abs(change / olderAvg) * 100;
      
      let direction = 'stable';
      let confidence = Math.min(changePercent / 5, 1); // 0-1 confidence
      
      if (changePercent > 2) {
        direction = change > 0 ? 'increasing' : 'decreasing';
      }
      
      this.trends.set(kpi, { direction, confidence, change });
    });
  }

  async startWebServer() {
    this.server = http.createServer((req, res) => {
      this.handleRequest(req, res);
    });
    
    return new Promise((resolve) => {
      this.server.listen(DASHBOARD_CONFIG.port, () => {
        resolve();
      });
    });
  }

  handleRequest(req, res) {
    const parsedUrl = url.parse(req.url, true);
    const pathname = parsedUrl.pathname;
    
    // Set CORS headers
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
    
    if (req.method === 'OPTIONS') {
      res.writeHead(200);
      res.end();
      return;
    }
    
    switch (pathname) {
      case '/dashboard':
        this.serveDashboard(res);
        break;
      case '/api/metrics':
        this.serveMetricsAPI(res);
        break;
      case '/api/kpis':
        this.serveKPIsAPI(res);
        break;
      case '/api/alerts':
        this.serveAlertsAPI(res);
        break;
      case '/api/trends':
        this.serveTrendsAPI(res);
        break;
      case '/api/status':
        this.serveStatusAPI(res);
        break;
      default:
        res.writeHead(404, { 'Content-Type': 'text/plain' });
        res.end('Not Found');
    }
  }

  serveDashboard(res) {
    const html = this.generateDashboardHTML();
    res.writeHead(200, { 'Content-Type': 'text/html' });
    res.end(html);
  }

  serveMetricsAPI(res) {
    const metricsData = this.getMetricsData();
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(metricsData, null, 2));
  }

  serveKPIsAPI(res) {
    const kpiData = this.getKPIData();
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(kpiData, null, 2));
  }

  serveAlertsAPI(res) {
    const activeAlerts = this.alerts.filter(alert => !alert.resolved);
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(activeAlerts, null, 2));
  }

  serveTrendsAPI(res) {
    const trendsData = Object.fromEntries(this.trends);
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(trendsData, null, 2));
  }

  serveStatusAPI(res) {
    const status = this.getOverallStatus();
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(status, null, 2));
  }

  getMetricsData() {
    const data = {};
    
    this.kpiHistory.forEach((history, kpi) => {
      data[kpi] = {
        current: history.length > 0 ? history[history.length - 1] : null,
        history: history.slice(-50), // Last 50 points
        config: DASHBOARD_CONFIG.kpis[kpi]
      };
    });
    
    return data;
  }

  getKPIData() {
    const kpis = {};
    
    Object.keys(DASHBOARD_CONFIG.kpis).forEach(kpi => {
      const history = this.kpiHistory.get(kpi) || [];
      const current = history.length > 0 ? history[history.length - 1] : null;
      const trend = this.trends.get(kpi) || { direction: 'stable', confidence: 0 };
      
      kpis[kpi] = {
        name: kpi.replace(/([A-Z])/g, ' $1').toLowerCase(),
        current: current ? current.value : null,
        status: current ? current.status : 'unknown',
        target: DASHBOARD_CONFIG.kpis[kpi].target,
        unit: DASHBOARD_CONFIG.kpis[kpi].unit,
        trend: trend.direction,
        confidence: trend.confidence
      };
    });
    
    return kpis;
  }

  getOverallStatus() {
    const kpiStatuses = [];
    
    this.kpiHistory.forEach((history, kpi) => {
      if (history.length > 0) {
        kpiStatuses.push(history[history.length - 1].status);
      }
    });
    
    const criticalCount = kpiStatuses.filter(s => s === 'critical').length;
    const warningCount = kpiStatuses.filter(s => s === 'warning').length;
    const goodCount = kpiStatuses.filter(s => s === 'good' || s === 'excellent').length;
    
    let overallStatus = 'healthy';
    if (criticalCount > 0) {
      overallStatus = 'critical';
    } else if (warningCount > goodCount) {
      overallStatus = 'warning';
    }
    
    return {
      status: overallStatus,
      timestamp: new Date().toISOString(),
      summary: {
        total: kpiStatuses.length,
        critical: criticalCount,
        warning: warningCount,
        good: goodCount,
        unknown: kpiStatuses.filter(s => s === 'unknown').length
      },
      activeAlerts: this.alerts.filter(alert => !alert.resolved).length
    };
  }

  generateDashboardHTML() {
    const kpiData = this.getKPIData();
    const status = this.getOverallStatus();
    
    return `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>QA Metrics Dashboard</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { 
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: #333;
            min-height: 100vh;
        }
        .header {
            background: rgba(255, 255, 255, 0.1);
            backdrop-filter: blur(10px);
            padding: 20px;
            text-align: center;
            color: white;
            border-bottom: 1px solid rgba(255, 255, 255, 0.2);
        }
        .header h1 { font-size: 2.5em; margin-bottom: 10px; }
        .header .subtitle { font-size: 1.1em; opacity: 0.9; }
        .status-bar {
            background: ${this.getStatusColor(status.status)};
            color: white;
            padding: 15px;
            text-align: center;
            font-weight: bold;
            font-size: 1.1em;
        }
        .container { 
            max-width: 1400px; 
            margin: 0 auto; 
            padding: 30px 20px;
        }
        .metrics-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }
        .metric-card {
            background: white;
            border-radius: 12px;
            padding: 25px;
            box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
            transition: transform 0.2s, box-shadow 0.2s;
        }
        .metric-card:hover {
            transform: translateY(-2px);
            box-shadow: 0 12px 35px rgba(0, 0, 0, 0.15);
        }
        .metric-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 15px;
        }
        .metric-name {
            font-size: 1.1em;
            font-weight: 600;
            text-transform: capitalize;
        }
        .metric-status {
            padding: 4px 12px;
            border-radius: 20px;
            font-size: 0.8em;
            font-weight: bold;
            text-transform: uppercase;
        }
        .status-excellent { background: ${DASHBOARD_CONFIG.colors.excellent}; color: white; }
        .status-good { background: ${DASHBOARD_CONFIG.colors.good}; color: white; }
        .status-warning { background: ${DASHBOARD_CONFIG.colors.warning}; color: white; }
        .status-critical { background: ${DASHBOARD_CONFIG.colors.critical}; color: white; }
        .status-unknown { background: ${DASHBOARD_CONFIG.colors.unknown}; color: white; }
        .metric-value {
            font-size: 2.5em;
            font-weight: bold;
            margin-bottom: 10px;
            color: #2c3e50;
        }
        .metric-target {
            color: #666;
            font-size: 0.9em;
            margin-bottom: 15px;
        }
        .metric-trend {
            display: flex;
            align-items: center;
            font-size: 0.9em;
            color: #666;
        }
        .trend-arrow {
            margin-right: 5px;
            font-weight: bold;
        }
        .trend-increasing { color: #00C851; }
        .trend-decreasing { color: #ff4444; }
        .trend-stable { color: #666; }
        .alerts-section {
            background: white;
            border-radius: 12px;
            padding: 25px;
            box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
            margin-top: 20px;
        }
        .section-title {
            font-size: 1.5em;
            font-weight: 600;
            margin-bottom: 20px;
            color: #2c3e50;
        }
        .alert-item {
            background: #fff5f5;
            border-left: 4px solid #ff4444;
            padding: 15px;
            margin-bottom: 10px;
            border-radius: 6px;
        }
        .alert-high { border-left-color: #ff4444; }
        .alert-medium { border-left-color: #ffbb33; }
        .alert-time {
            color: #666;
            font-size: 0.8em;
            margin-top: 5px;
        }
        .footer {
            text-align: center;
            padding: 20px;
            color: rgba(255, 255, 255, 0.7);
            font-size: 0.9em;
        }
        .refresh-info {
            position: fixed;
            top: 20px;
            right: 20px;
            background: rgba(255, 255, 255, 0.9);
            padding: 10px 15px;
            border-radius: 25px;
            font-size: 0.8em;
            color: #333;
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>🎯 QA Metrics Dashboard</h1>
        <div class="subtitle">Enterprise Quality Assurance - Real-time Monitoring</div>
    </div>
    
    <div class="status-bar">
        System Status: ${status.status.toUpperCase()} | 
        Active Alerts: ${status.activeAlerts} | 
        Last Updated: ${new Date().toLocaleTimeString()}
    </div>
    
    <div class="container">
        <div class="metrics-grid">
            ${Object.entries(kpiData).map(([kpi, data]) => `
                <div class="metric-card">
                    <div class="metric-header">
                        <div class="metric-name">${data.name}</div>
                        <div class="metric-status status-${data.status}">${data.status}</div>
                    </div>
                    <div class="metric-value">
                        ${data.current || 'N/A'}${data.current ? data.unit : ''}
                    </div>
                    <div class="metric-target">
                        Target: ${data.target}${data.unit}
                    </div>
                    <div class="metric-trend trend-${data.trend}">
                        <span class="trend-arrow">
                            ${data.trend === 'increasing' ? '↗️' : data.trend === 'decreasing' ? '↘️' : '➡️'}
                        </span>
                        ${data.trend} (${Math.round(data.confidence * 100)}% confidence)
                    </div>
                </div>
            `).join('')}
        </div>
        
        <div class="alerts-section">
            <h2 class="section-title">🚨 Active Alerts</h2>
            ${this.alerts.filter(alert => !alert.resolved).slice(0, 5).map(alert => `
                <div class="alert-item alert-${alert.severity}">
                    <strong>${alert.kpi.replace(/([A-Z])/g, ' $1').toLowerCase()}:</strong> ${alert.message}
                    <div class="alert-time">${new Date(alert.timestamp).toLocaleString()}</div>
                </div>
            `).join('') || '<p>No active alerts 🎉</p>'}
        </div>
    </div>
    
    <div class="refresh-info">
        🔄 Auto-refresh every ${DASHBOARD_CONFIG.refreshInterval / 1000}s
    </div>
    
    <div class="footer">
        Enterprise QA Dashboard v2.0 | Powered by Quality Metrics System
    </div>
    
    <script>
        // Auto-refresh the page
        setTimeout(() => {
            location.reload();
        }, ${DASHBOARD_CONFIG.refreshInterval});
    </script>
</body>
</html>`;
  }

  getStatusColor(status) {
    switch (status) {
      case 'critical': return '#ff4444';
      case 'warning': return '#ffbb33';
      case 'healthy': return '#00C851';
      default: return '#666666';
    }
  }

  async saveCurrentState() {
    const data = {
      timestamp: new Date().toISOString(),
      kpiHistory: Object.fromEntries(this.kpiHistory),
      alerts: this.alerts,
      reviews: this.reviews,
      trends: Object.fromEntries(this.trends)
    };
    
    fs.writeFileSync('coverage/dashboard-data.json', JSON.stringify(data, null, 2));
    console.log('💾 Dashboard state saved');
  }

  async generateReport() {
    const report = {
      timestamp: new Date().toISOString(),
      period: '24h',
      overallStatus: this.getOverallStatus(),
      kpiSummary: this.getKPIData(),
      trends: Object.fromEntries(this.trends),
      alerts: {
        total: this.alerts.length,
        active: this.alerts.filter(alert => !alert.resolved).length,
        resolved: this.alerts.filter(alert => alert.resolved).length,
        byKPI: this.getAlertsByKPI()
      },
      recommendations: this.generateRecommendations()
    };

    const reportPath = `coverage/dashboard-report-${Date.now()}.json`;
    fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
    
    console.log(`📄 Dashboard report saved: ${reportPath}`);
    
    return report;
  }

  getAlertsByKPI() {
    const alertsByKPI = {};
    
    this.alerts.forEach(alert => {
      if (!alertsByKPI[alert.kpi]) {
        alertsByKPI[alert.kpi] = 0;
      }
      alertsByKPI[alert.kpi]++;
    });
    
    return alertsByKPI;
  }

  generateRecommendations() {
    const recommendations = [];
    
    // Analyze current KPI status and generate recommendations
    Object.keys(DASHBOARD_CONFIG.kpis).forEach(kpi => {
      const history = this.kpiHistory.get(kpi) || [];
      if (history.length === 0) return;
      
      const current = history[history.length - 1];
      const trend = this.trends.get(kpi);
      
      if (current.status === 'critical') {
        recommendations.push({
          priority: 'high',
          kpi,
          issue: `${kpi} is in critical state`,
          action: this.getRecommendedAction(kpi, 'critical'),
          impact: 'immediate'
        });
      } else if (current.status === 'warning') {
        recommendations.push({
          priority: 'medium',
          kpi,
          issue: `${kpi} is below target`,
          action: this.getRecommendedAction(kpi, 'warning'),
          impact: 'short-term'
        });
      }
      
      if (trend && trend.direction === 'decreasing' && trend.confidence > 0.7) {
        recommendations.push({
          priority: 'medium',
          kpi,
          issue: `${kpi} is trending downward`,
          action: 'Monitor closely and investigate root cause',
          impact: 'long-term'
        });
      }
    });
    
    return recommendations;
  }

  getRecommendedAction(kpi, severity) {
    const actions = {
      bugEscapeRate: {
        critical: 'Immediate code review and testing process audit required',
        warning: 'Increase test coverage and implement additional code review steps'
      },
      testCoverage: {
        critical: 'Stop deployment until test coverage improves to minimum threshold',
        warning: 'Add missing unit tests for uncovered code paths'
      },
      performanceScore: {
        critical: 'Performance optimization required before next release',
        warning: 'Review and optimize slow-performing components'
      },
      securityScore: {
        critical: 'Security audit and vulnerability remediation required',
        warning: 'Address identified security issues and update dependencies'
      },
      codeQuality: {
        critical: 'Code refactoring and quality improvement sprint needed',
        warning: 'Address linting issues and improve code documentation'
      },
      deploymentSuccess: {
        critical: 'Deployment pipeline investigation and fixes required',
        warning: 'Review recent deployment failures and improve automation'
      },
      mttr: {
        critical: 'Incident response process improvement needed',
        warning: 'Enhance monitoring and alerting systems'
      },
      reviewApprovalRate: {
        critical: 'Review process evaluation and training required',
        warning: 'Improve code quality before review submission'
      }
    };
    
    return actions[kpi]?.[severity] || 'Monitor and investigate';
  }
}

// CLI interface
if (require.main === module) {
  const command = process.argv[2];
  const dashboard = new QualityMetricsDashboard();

  switch (command) {
    case 'start':
      dashboard.start().catch(console.error);
      break;
    case 'report':
      dashboard.generateReport()
        .then(() => console.log('Report generated'))
        .catch(console.error);
      break;
    default:
      console.log('Usage: node metrics-dashboard.js <start|report>');
  }

  // Graceful shutdown
  process.on('SIGINT', () => {
    dashboard.stop().then(() => process.exit(0));
  });
  
  process.on('SIGTERM', () => {
    dashboard.stop().then(() => process.exit(0));
  });
}

module.exports = QualityMetricsDashboard;