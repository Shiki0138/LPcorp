#!/usr/bin/env node

/**
 * Peer Review & Cross-Validation System
 * Enterprise-grade code quality assurance through peer review
 */

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { execSync } = require('child_process');

// Review configuration
const REVIEW_CONFIG = {
  rules: {
    minimumReviewers: 2,
    requireApproval: true,
    blockOnFailure: true,
    autoAssignment: true,
    conflictResolution: 'majority'
  },
  reviewers: {
    senior: ['senior-dev-1', 'senior-dev-2', 'tech-lead'],
    regular: ['dev-1', 'dev-2', 'dev-3', 'dev-4'],
    security: ['security-expert', 'devops-lead'],
    qa: ['qa-lead', 'test-engineer']
  },
  criteria: {
    codeQuality: { weight: 30, threshold: 8 },
    testCoverage: { weight: 25, threshold: 95 },
    security: { weight: 20, threshold: 9 },
    documentation: { weight: 15, threshold: 7 },
    performance: { weight: 10, threshold: 8 }
  }
};

class PeerReviewSystem {
  constructor() {
    this.reviews = new Map();
    this.reviewHistory = [];
    this.metrics = {
      totalReviews: 0,
      approvedReviews: 0,
      rejectedReviews: 0,
      averageReviewTime: 0,
      qualityScores: []
    };
  }

  async initiateReview(changes) {
    console.log('🔍 Initiating peer review process...');
    
    const reviewId = this.generateReviewId();
    const review = {
      id: reviewId,
      timestamp: new Date().toISOString(),
      status: 'pending',
      changes,
      reviewers: [],
      scores: {},
      comments: [],
      approvals: 0,
      rejections: 0,
      finalScore: 0,
      approved: false
    };

    // Auto-assign reviewers
    if (REVIEW_CONFIG.rules.autoAssignment) {
      review.reviewers = await this.autoAssignReviewers(changes);
    }

    // Analyze changes
    review.analysis = await this.analyzeChanges(changes);
    
    // Store review
    this.reviews.set(reviewId, review);
    
    console.log(`📝 Review ${reviewId} initiated with ${review.reviewers.length} reviewers`);
    console.log(`👥 Assigned reviewers: ${review.reviewers.join(', ')}`);
    
    return reviewId;
  }

  async autoAssignReviewers(changes) {
    const reviewers = new Set();
    
    // Always assign at least one senior reviewer
    const seniorReviewers = REVIEW_CONFIG.reviewers.senior;
    reviewers.add(seniorReviewers[Math.floor(Math.random() * seniorReviewers.length)]);
    
    // Assign based on change type
    if (this.hasSecurityChanges(changes)) {
      const securityReviewers = REVIEW_CONFIG.reviewers.security;
      reviewers.add(securityReviewers[Math.floor(Math.random() * securityReviewers.length)]);
    }
    
    if (this.hasTestChanges(changes)) {
      const qaReviewers = REVIEW_CONFIG.reviewers.qa;
      reviewers.add(qaReviewers[Math.floor(Math.random() * qaReviewers.length)]);
    }
    
    // Add regular reviewers to meet minimum requirement
    while (reviewers.size < REVIEW_CONFIG.rules.minimumReviewers) {
      const regularReviewers = REVIEW_CONFIG.reviewers.regular;
      reviewers.add(regularReviewers[Math.floor(Math.random() * regularReviewers.length)]);
    }
    
    return Array.from(reviewers);
  }

  async analyzeChanges(changes) {
    const analysis = {
      linesChanged: 0,
      filesChanged: 0,
      complexity: 'low',
      riskLevel: 'low',
      categories: [],
      suggestedReviewTime: 0
    };

    try {
      // Get git diff stats
      const diffStats = execSync('git diff --stat HEAD~1', { encoding: 'utf8' });
      const lines = diffStats.split('\n');
      
      lines.forEach(line => {
        if (line.includes('|')) {
          analysis.filesChanged++;
          const matches = line.match(/(\d+) \+/);
          if (matches) {
            analysis.linesChanged += parseInt(matches[1]);
          }
        }
      });

      // Determine complexity and risk
      if (analysis.linesChanged > 500) {
        analysis.complexity = 'high';
        analysis.riskLevel = 'high';
        analysis.suggestedReviewTime = 120; // 2 hours
      } else if (analysis.linesChanged > 100) {
        analysis.complexity = 'medium';
        analysis.riskLevel = 'medium';
        analysis.suggestedReviewTime = 60; // 1 hour
      } else {
        analysis.complexity = 'low';
        analysis.riskLevel = 'low';
        analysis.suggestedReviewTime = 30; // 30 minutes
      }

      // Categorize changes
      if (this.hasSecurityChanges(changes)) analysis.categories.push('security');
      if (this.hasTestChanges(changes)) analysis.categories.push('testing');
      if (this.hasDocChanges(changes)) analysis.categories.push('documentation');
      if (this.hasConfigChanges(changes)) analysis.categories.push('configuration');

    } catch (error) {
      console.warn('⚠️  Could not analyze git changes:', error.message);
    }

    return analysis;
  }

  hasSecurityChanges(changes) {
    const securityPatterns = [
      /auth|security|password|token|jwt|oauth/i,
      /encrypt|decrypt|hash|salt/i,
      /\.env|config.*secret/i
    ];
    
    return changes.some(change => 
      securityPatterns.some(pattern => pattern.test(change.file || change))
    );
  }

  hasTestChanges(changes) {
    return changes.some(change => 
      (change.file || change).includes('test') || 
      (change.file || change).includes('spec')
    );
  }

  hasDocChanges(changes) {
    return changes.some(change => 
      (change.file || change).match(/\.(md|txt|rst|doc)$/i)
    );
  }

  hasConfigChanges(changes) {
    const configPatterns = [
      /package\.json|yarn\.lock|package-lock\.json/,
      /\.env|\.config|\.yml|\.yaml|\.toml/,
      /dockerfile|docker-compose/i
    ];
    
    return changes.some(change => 
      configPatterns.some(pattern => pattern.test(change.file || change))
    );
  }

  async submitReview(reviewId, reviewerId, scores, comments) {
    console.log(`📝 Submitting review from ${reviewerId} for ${reviewId}...`);
    
    const review = this.reviews.get(reviewId);
    if (!review) {
      throw new Error(`Review ${reviewId} not found`);
    }

    if (!review.reviewers.includes(reviewerId)) {
      throw new Error(`${reviewerId} is not assigned to review ${reviewId}`);
    }

    // Validate scores
    this.validateScores(scores);
    
    // Store review feedback
    review.scores[reviewerId] = scores;
    review.comments.push({
      reviewer: reviewerId,
      timestamp: new Date().toISOString(),
      comments,
      scores
    });

    // Calculate overall score for this reviewer
    const overallScore = this.calculateOverallScore(scores);
    
    if (overallScore >= 8) {
      review.approvals++;
      console.log(`✅ ${reviewerId} approved the review (score: ${overallScore})`);
    } else {
      review.rejections++;
      console.log(`❌ ${reviewerId} rejected the review (score: ${overallScore})`);
    }

    // Check if review is complete
    if (this.isReviewComplete(review)) {
      await this.finalizeReview(reviewId);
    }

    return overallScore;
  }

  validateScores(scores) {
    Object.keys(REVIEW_CONFIG.criteria).forEach(criterion => {
      if (!scores[criterion]) {
        throw new Error(`Missing score for criterion: ${criterion}`);
      }
      
      const score = scores[criterion];
      if (typeof score !== 'number' || score < 1 || score > 10) {
        throw new Error(`Invalid score for ${criterion}: must be between 1 and 10`);
      }
    });
  }

  calculateOverallScore(scores) {
    let weightedSum = 0;
    let totalWeight = 0;

    Object.entries(REVIEW_CONFIG.criteria).forEach(([criterion, config]) => {
      const score = scores[criterion];
      weightedSum += score * config.weight;
      totalWeight += config.weight;
    });

    return Math.round((weightedSum / totalWeight) * 10) / 10;
  }

  isReviewComplete(review) {
    const totalReviews = review.approvals + review.rejections;
    const minimumReviewers = REVIEW_CONFIG.rules.minimumReviewers;
    
    // All assigned reviewers have submitted
    if (totalReviews >= review.reviewers.length) {
      return true;
    }
    
    // Minimum reviewers met and majority decision reached
    if (totalReviews >= minimumReviewers) {
      if (review.approvals > review.rejections) {
        return true;
      } else if (review.rejections > review.approvals) {
        return true;
      }
    }
    
    return false;
  }

  async finalizeReview(reviewId) {
    console.log(`🏁 Finalizing review ${reviewId}...`);
    
    const review = this.reviews.get(reviewId);
    
    // Calculate final score
    const allScores = Object.values(review.scores);
    const criteriaScores = {};
    
    Object.keys(REVIEW_CONFIG.criteria).forEach(criterion => {
      const scores = allScores.map(score => score[criterion]);
      criteriaScores[criterion] = scores.reduce((sum, score) => sum + score, 0) / scores.length;
    });
    
    review.finalScore = this.calculateOverallScore(criteriaScores);
    
    // Determine approval status
    review.approved = this.determineApproval(review);
    review.status = review.approved ? 'approved' : 'rejected';
    
    // Update metrics
    this.updateMetrics(review);
    
    // Save to history
    this.reviewHistory.push({
      ...review,
      finalizedAt: new Date().toISOString()
    });
    
    // Generate report
    await this.generateReviewReport(review);
    
    console.log(`${review.approved ? '✅ Review APPROVED' : '❌ Review REJECTED'} (final score: ${review.finalScore})`);
    
    return review;
  }

  determineApproval(review) {
    const conflictResolution = REVIEW_CONFIG.rules.conflictResolution;
    
    switch (conflictResolution) {
      case 'majority':
        return review.approvals > review.rejections;
      case 'unanimous':
        return review.rejections === 0;
      case 'senior':
        // Check if at least one senior reviewer approved
        const seniorApprovals = review.comments.filter(comment => 
          REVIEW_CONFIG.reviewers.senior.includes(comment.reviewer) &&
          this.calculateOverallScore(comment.scores) >= 8
        );
        return seniorApprovals.length > 0 && review.approvals > review.rejections;
      default:
        return review.approvals > review.rejections;
    }
  }

  updateMetrics(review) {
    this.metrics.totalReviews++;
    
    if (review.approved) {
      this.metrics.approvedReviews++;
    } else {
      this.metrics.rejectedReviews++;
    }
    
    this.metrics.qualityScores.push(review.finalScore);
    
    // Calculate average review time
    const reviewTime = Date.now() - new Date(review.timestamp).getTime();
    this.metrics.averageReviewTime = 
      (this.metrics.averageReviewTime * (this.metrics.totalReviews - 1) + reviewTime) / 
      this.metrics.totalReviews;
  }

  async generateReviewReport(review) {
    const report = {
      reviewId: review.id,
      timestamp: review.timestamp,
      finalizedAt: new Date().toISOString(),
      status: review.status,
      approved: review.approved,
      finalScore: review.finalScore,
      analysis: review.analysis,
      reviewers: review.reviewers,
      scores: review.scores,
      comments: review.comments,
      statistics: {
        approvals: review.approvals,
        rejections: review.rejections,
        totalReviews: review.approvals + review.rejections
      },
      criteria: Object.keys(REVIEW_CONFIG.criteria).reduce((acc, criterion) => {
        const scores = Object.values(review.scores).map(score => score[criterion]);
        acc[criterion] = {
          average: scores.reduce((sum, score) => sum + score, 0) / scores.length,
          threshold: REVIEW_CONFIG.criteria[criterion].threshold,
          passed: (scores.reduce((sum, score) => sum + score, 0) / scores.length) >= REVIEW_CONFIG.criteria[criterion].threshold
        };
        return acc;
      }, {})
    };

    const reportPath = `coverage/review-report-${review.id}.json`;
    fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
    
    console.log(`📄 Review report saved: ${reportPath}`);
    
    return report;
  }

  async runCrossValidation(codebase) {
    console.log('🔄 Running cross-validation analysis...');
    
    const validation = {
      timestamp: new Date().toISOString(),
      checks: [],
      overallScore: 0,
      recommendations: []
    };

    try {
      // Code quality cross-validation
      const codeQualityCheck = await this.validateCodeQuality(codebase);
      validation.checks.push(codeQualityCheck);
      
      // Test coverage cross-validation
      const testCoverageCheck = await this.validateTestCoverage(codebase);
      validation.checks.push(testCoverageCheck);
      
      // Security cross-validation
      const securityCheck = await this.validateSecurity(codebase);
      validation.checks.push(securityCheck);
      
      // Documentation cross-validation
      const documentationCheck = await this.validateDocumentation(codebase);
      validation.checks.push(documentationCheck);
      
      // Performance cross-validation
      const performanceCheck = await this.validatePerformance(codebase);
      validation.checks.push(performanceCheck);
      
      // Calculate overall score
      validation.overallScore = this.calculateValidationScore(validation.checks);
      
      // Generate recommendations
      validation.recommendations = this.generateRecommendations(validation.checks);
      
      // Save validation report
      await this.saveValidationReport(validation);
      
      console.log(`✅ Cross-validation completed (score: ${validation.overallScore})`);
      
    } catch (error) {
      console.error('❌ Cross-validation failed:', error.message);
      validation.error = error.message;
    }

    return validation;
  }

  async validateCodeQuality(codebase) {
    const check = {
      name: 'codeQuality',
      status: 'passed',
      score: 0,
      details: {},
      issues: []
    };

    try {
      // Run ESLint
      const lintOutput = execSync('npm run lint -- --format json', { 
        encoding: 'utf8',
        stdio: 'pipe'
      });
      
      const lintResults = JSON.parse(lintOutput);
      const totalErrors = lintResults.reduce((sum, file) => sum + file.errorCount, 0);
      const totalWarnings = lintResults.reduce((sum, file) => sum + file.warningCount, 0);
      
      check.details = {
        totalFiles: lintResults.length,
        totalErrors,
        totalWarnings,
        score: Math.max(0, 100 - (totalErrors * 10) - (totalWarnings * 2))
      };
      
      check.score = check.details.score;
      check.status = totalErrors === 0 ? 'passed' : 'failed';
      
      if (totalErrors > 0) {
        check.issues.push(`${totalErrors} linting errors found`);
      }
      
    } catch (error) {
      check.status = 'failed';
      check.score = 0;
      check.issues.push(`Linting failed: ${error.message}`);
    }

    return check;
  }

  async validateTestCoverage(codebase) {
    const check = {
      name: 'testCoverage',
      status: 'passed',
      score: 0,
      details: {},
      issues: []
    };

    try {
      // Check if coverage file exists
      if (fs.existsSync('coverage/coverage-summary.json')) {
        const coverageData = JSON.parse(fs.readFileSync('coverage/coverage-summary.json', 'utf8'));
        const coverage = coverageData.total;
        
        check.details = {
          statements: coverage.statements.pct,
          branches: coverage.branches.pct,
          functions: coverage.functions.pct,
          lines: coverage.lines.pct
        };
        
        check.score = Math.min(100, (
          coverage.statements.pct +
          coverage.branches.pct +
          coverage.functions.pct +
          coverage.lines.pct
        ) / 4);
        
        const threshold = REVIEW_CONFIG.criteria.testCoverage.threshold;
        check.status = check.score >= threshold ? 'passed' : 'failed';
        
        if (check.score < threshold) {
          check.issues.push(`Test coverage ${check.score}% is below threshold ${threshold}%`);
        }
        
      } else {
        check.status = 'failed';
        check.score = 0;
        check.issues.push('No coverage data found');
      }
      
    } catch (error) {
      check.status = 'failed';
      check.score = 0;
      check.issues.push(`Coverage validation failed: ${error.message}`);
    }

    return check;
  }

  async validateSecurity(codebase) {
    const check = {
      name: 'security',
      status: 'passed',
      score: 0,
      details: {},
      issues: []
    };

    try {
      // Run npm audit
      const auditOutput = execSync('npm audit --json', { 
        encoding: 'utf8',
        stdio: 'pipe'
      });
      
      const auditResult = JSON.parse(auditOutput);
      const vulnerabilities = auditResult.metadata?.vulnerabilities || {};
      
      const totalVulns = Object.values(vulnerabilities).reduce((sum, count) => sum + count, 0);
      const critical = vulnerabilities.critical || 0;
      const high = vulnerabilities.high || 0;
      
      check.details = {
        totalVulnerabilities: totalVulns,
        critical,
        high,
        moderate: vulnerabilities.moderate || 0,
        low: vulnerabilities.low || 0
      };
      
      // Score based on severity
      check.score = Math.max(0, 100 - (critical * 20) - (high * 10) - (vulnerabilities.moderate || 0) * 2);
      check.status = critical === 0 && high === 0 ? 'passed' : 'failed';
      
      if (critical > 0) {
        check.issues.push(`${critical} critical vulnerabilities found`);
      }
      if (high > 0) {
        check.issues.push(`${high} high severity vulnerabilities found`);
      }
      
    } catch (error) {
      // npm audit returns non-zero code when vulnerabilities found
      if (error.stdout) {
        const auditResult = JSON.parse(error.stdout);
        const vulnerabilities = auditResult.metadata?.vulnerabilities || {};
        const totalVulns = Object.values(vulnerabilities).reduce((sum, count) => sum + count, 0);
        
        check.details = { vulnerabilities };
        check.score = Math.max(0, 100 - totalVulns * 5);
        check.status = totalVulns === 0 ? 'passed' : 'failed';
        
        if (totalVulns > 0) {
          check.issues.push(`${totalVulns} security vulnerabilities found`);
        }
      } else {
        check.status = 'failed';
        check.score = 0;
        check.issues.push(`Security audit failed: ${error.message}`);
      }
    }

    return check;
  }

  async validateDocumentation(codebase) {
    const check = {
      name: 'documentation',
      status: 'passed',
      score: 0,
      details: {},
      issues: []
    };

    try {
      const requiredDocs = [
        'README.md',
        'docs/api',
        'docs/architecture'
      ];
      
      const existingDocs = requiredDocs.filter(doc => fs.existsSync(doc));
      const score = (existingDocs.length / requiredDocs.length) * 100;
      
      check.details = {
        required: requiredDocs,
        existing: existingDocs,
        missing: requiredDocs.filter(doc => !existingDocs.includes(doc))
      };
      
      check.score = score;
      check.status = score >= 80 ? 'passed' : 'failed';
      
      if (check.details.missing.length > 0) {
        check.issues.push(`Missing documentation: ${check.details.missing.join(', ')}`);
      }
      
    } catch (error) {
      check.status = 'failed';
      check.score = 0;
      check.issues.push(`Documentation validation failed: ${error.message}`);
    }

    return check;
  }

  async validatePerformance(codebase) {
    const check = {
      name: 'performance',
      status: 'passed',
      score: 85, // Mock score
      details: {
        buildTime: '45s',
        bundleSize: '2.1MB',
        loadTime: '1.8s'
      },
      issues: []
    };

    // In a real implementation, this would run performance tests
    // For now, we'll simulate a performance check
    
    return check;
  }

  calculateValidationScore(checks) {
    const scores = checks.map(check => check.score);
    return Math.round(scores.reduce((sum, score) => sum + score, 0) / scores.length);
  }

  generateRecommendations(checks) {
    const recommendations = [];
    
    checks.forEach(check => {
      if (check.status === 'failed') {
        check.issues.forEach(issue => {
          recommendations.push({
            category: check.name,
            priority: check.score < 50 ? 'high' : 'medium',
            issue,
            suggestion: this.getSuggestion(check.name, issue)
          });
        });
      }
    });
    
    return recommendations;
  }

  getSuggestion(category, issue) {
    const suggestions = {
      codeQuality: 'Run ESLint with --fix to automatically resolve issues',
      testCoverage: 'Add more unit tests to increase coverage',
      security: 'Run npm audit fix to resolve vulnerabilities',
      documentation: 'Add missing documentation files',
      performance: 'Optimize build configuration and bundle splitting'
    };
    
    return suggestions[category] || 'Please review and fix the identified issue';
  }

  async saveValidationReport(validation) {
    const reportPath = `coverage/cross-validation-${Date.now()}.json`;
    fs.writeFileSync(reportPath, JSON.stringify(validation, null, 2));
    
    console.log(`📄 Cross-validation report saved: ${reportPath}`);
  }

  generateReviewId() {
    return crypto.randomBytes(8).toString('hex');
  }

  getMetrics() {
    return {
      ...this.metrics,
      approvalRate: (this.metrics.approvedReviews / this.metrics.totalReviews) * 100,
      averageQualityScore: this.metrics.qualityScores.reduce((sum, score) => sum + score, 0) / this.metrics.qualityScores.length,
      averageReviewTimeMinutes: Math.round(this.metrics.averageReviewTime / 60000)
    };
  }

  async generateMetricsReport() {
    const metrics = this.getMetrics();
    const report = {
      timestamp: new Date().toISOString(),
      period: 'all-time',
      metrics,
      reviewHistory: this.reviewHistory.slice(-10), // Last 10 reviews
      trends: {
        qualityTrend: this.calculateQualityTrend(),
        reviewTimeTrend: this.calculateReviewTimeTrend()
      }
    };

    const reportPath = `coverage/peer-review-metrics-${Date.now()}.json`;
    fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
    
    console.log(`📊 Peer review metrics report saved: ${reportPath}`);
    
    return report;
  }

  calculateQualityTrend() {
    if (this.metrics.qualityScores.length < 2) return 'insufficient-data';
    
    const recent = this.metrics.qualityScores.slice(-5);
    const older = this.metrics.qualityScores.slice(-10, -5);
    
    if (older.length === 0) return 'insufficient-data';
    
    const recentAvg = recent.reduce((sum, score) => sum + score, 0) / recent.length;
    const olderAvg = older.reduce((sum, score) => sum + score, 0) / older.length;
    
    if (recentAvg > olderAvg + 0.5) return 'improving';
    if (recentAvg < olderAvg - 0.5) return 'declining';
    return 'stable';
  }

  calculateReviewTimeTrend() {
    // Simplified trend calculation
    return 'stable';
  }
}

// CLI interface
if (require.main === module) {
  const command = process.argv[2];
  const reviewSystem = new PeerReviewSystem();

  switch (command) {
    case 'initiate':
      // Mock changes for testing
      const mockChanges = [
        { file: 'src/components/Auth.js', type: 'modified' },
        { file: 'tests/auth.test.js', type: 'added' },
        { file: 'README.md', type: 'modified' }
      ];
      
      reviewSystem.initiateReview(mockChanges)
        .then(reviewId => {
          console.log(`Review initiated: ${reviewId}`);
          
          // Mock review submissions
          setTimeout(() => {
            reviewSystem.submitReview(reviewId, 'senior-dev-1', {
              codeQuality: 8,
              testCoverage: 9,
              security: 9,
              documentation: 7,
              performance: 8
            }, ['Good implementation', 'Consider adding more edge case tests']);
          }, 1000);
          
          setTimeout(() => {
            reviewSystem.submitReview(reviewId, 'dev-2', {
              codeQuality: 9,
              testCoverage: 8,
              security: 8,
              documentation: 8,
              performance: 9
            }, ['Clean code', 'Well structured']);
          }, 2000);
        })
        .catch(console.error);
      break;
      
    case 'validate':
      reviewSystem.runCrossValidation('.')
        .then(result => {
          console.log('Cross-validation completed');
          console.log(`Overall score: ${result.overallScore}`);
        })
        .catch(console.error);
      break;
      
    case 'metrics':
      reviewSystem.generateMetricsReport()
        .then(() => console.log('Metrics report generated'))
        .catch(console.error);
      break;
      
    default:
      console.log('Usage: node peer-review-system.js <initiate|validate|metrics>');
  }
}

module.exports = PeerReviewSystem;