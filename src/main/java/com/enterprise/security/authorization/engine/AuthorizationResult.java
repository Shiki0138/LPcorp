package com.enterprise.security.authorization.engine;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * Authorization result object
 */
public class AuthorizationResult {
    
    private boolean granted;
    private String reason;
    private String permissionSource; // direct, role, delegated
    private Instant timestamp;
    private Map<String, Object> metadata = new HashMap<>();
    private String userId;
    private String resourceId;
    private String action;
    
    // Risk assessment
    private RiskAssessment riskAssessment;
    
    // Audit information
    private String decisionPoint;
    private long evaluationTimeMs;
    
    // Constructors
    private AuthorizationResult() {
        this.timestamp = Instant.now();
    }
    
    private AuthorizationResult(boolean granted, String reason) {
        this();
        this.granted = granted;
        this.reason = reason;
    }
    
    // Static factory methods
    public static AuthorizationResult granted(String reason) {
        return new AuthorizationResult(true, reason);
    }
    
    public static AuthorizationResult denied(String reason) {
        return new AuthorizationResult(false, reason);
    }
    
    public static AuthorizationResult granted(String reason, String permissionSource) {
        AuthorizationResult result = new AuthorizationResult(true, reason);
        result.permissionSource = permissionSource;
        return result;
    }
    
    public static AuthorizationResult denied(String reason, RiskAssessment riskAssessment) {
        AuthorizationResult result = new AuthorizationResult(false, reason);
        result.riskAssessment = riskAssessment;
        return result;
    }
    
    // Builder methods
    public AuthorizationResult withMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }
    
    public AuthorizationResult withRequestInfo(String userId, String resourceId, String action) {
        this.userId = userId;
        this.resourceId = resourceId;
        this.action = action;
        return this;
    }
    
    public AuthorizationResult withRiskAssessment(RiskAssessment riskAssessment) {
        this.riskAssessment = riskAssessment;
        return this;
    }
    
    public AuthorizationResult withDecisionPoint(String decisionPoint) {
        this.decisionPoint = decisionPoint;
        return this;
    }
    
    public AuthorizationResult withEvaluationTime(long evaluationTimeMs) {
        this.evaluationTimeMs = evaluationTimeMs;
        return this;
    }
    
    // Business methods
    public boolean requiresAdditionalApproval() {
        return riskAssessment != null && riskAssessment.requiresApproval();
    }
    
    public boolean requiresAuditLogging() {
        return riskAssessment != null && riskAssessment.requiresAuditLogging();
    }
    
    public boolean isHighRisk() {
        return riskAssessment != null && riskAssessment.isHighRisk();
    }
    
    // Getters and setters
    public boolean isGranted() { return granted; }
    public void setGranted(boolean granted) { this.granted = granted; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getPermissionSource() { return permissionSource; }
    public void setPermissionSource(String permissionSource) { this.permissionSource = permissionSource; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public Map<String, Object> getMetadata() { return Collections.unmodifiableMap(metadata); }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public RiskAssessment getRiskAssessment() { return riskAssessment; }
    public void setRiskAssessment(RiskAssessment riskAssessment) { this.riskAssessment = riskAssessment; }
    
    public String getDecisionPoint() { return decisionPoint; }
    public void setDecisionPoint(String decisionPoint) { this.decisionPoint = decisionPoint; }
    
    public long getEvaluationTimeMs() { return evaluationTimeMs; }
    public void setEvaluationTimeMs(long evaluationTimeMs) { this.evaluationTimeMs = evaluationTimeMs; }
    
    public Object getMetadataValue(String key) {
        return metadata.get(key);
    }
    
    @Override
    public String toString() {
        return "AuthorizationResult{" +
                "granted=" + granted +
                ", reason='" + reason + '\'' +
                ", permissionSource='" + permissionSource + '\'' +
                ", timestamp=" + timestamp +
                ", userId='" + userId + '\'' +
                ", resourceId='" + resourceId + '\'' +
                ", action='" + action + '\'' +
                '}';
    }
    
    /**
     * Risk assessment information
     */
    public static class RiskAssessment {
        private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
        private double riskScore; // 0.0 to 1.0
        private String riskFactors;
        private boolean requiresApproval;
        private boolean requiresAuditLogging;
        private boolean requiresMfa;
        
        public RiskAssessment(String riskLevel, double riskScore) {
            this.riskLevel = riskLevel;
            this.riskScore = riskScore;
            this.requiresAuditLogging = riskScore >= 0.3; // Medium risk and above
            this.requiresApproval = riskScore >= 0.7; // High risk and above
            this.requiresMfa = riskScore >= 0.8; // Critical risk
        }
        
        public boolean isHighRisk() {
            return riskScore >= 0.7;
        }
        
        public boolean requiresApproval() {
            return requiresApproval;
        }
        
        public boolean requiresAuditLogging() {
            return requiresAuditLogging;
        }
        
        public boolean requiresMfa() {
            return requiresMfa;
        }
        
        // Getters and setters
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        
        public double getRiskScore() { return riskScore; }
        public void setRiskScore(double riskScore) { this.riskScore = riskScore; }
        
        public String getRiskFactors() { return riskFactors; }
        public void setRiskFactors(String riskFactors) { this.riskFactors = riskFactors; }
        
        public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }
        public void setRequiresAuditLogging(boolean requiresAuditLogging) { this.requiresAuditLogging = requiresAuditLogging; }
        public void setRequiresMfa(boolean requiresMfa) { this.requiresMfa = requiresMfa; }
    }
}