package com.enterprise.security.authorization.engine;

import java.util.Map;
import java.util.HashMap;

/**
 * Authorization request object
 */
public class AuthorizationRequest {
    
    private String userId;
    private String action;
    private String resourceId;
    private String resourceType;
    private Map<String, Object> context = new HashMap<>();
    private String clientIp;
    private String userAgent;
    private String sessionId;
    private String tenantId;
    private String countryCode;
    private boolean emergencyAccess = false;
    private Map<String, String> attributes = new HashMap<>();
    
    // Constructors
    public AuthorizationRequest() {}
    
    public AuthorizationRequest(String userId, String action, String resourceId) {
        this.userId = userId;
        this.action = action;
        this.resourceId = resourceId;
    }
    
    public AuthorizationRequest(String userId, String action, String resourceId, String resourceType) {
        this.userId = userId;
        this.action = action;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
    }
    
    // Builder pattern
    public static AuthorizationRequestBuilder builder() {
        return new AuthorizationRequestBuilder();
    }
    
    public AuthorizationRequest withContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }
    
    public AuthorizationRequest withAttribute(String key, String value) {
        this.attributes.put(key, value);
        return this;
    }
    
    public AuthorizationRequest withClientInfo(String clientIp, String userAgent) {
        this.clientIp = clientIp;
        this.userAgent = userAgent;
        return this;
    }
    
    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    
    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }
    
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    
    public boolean isEmergencyAccess() { return emergencyAccess; }
    public void setEmergencyAccess(boolean emergencyAccess) { this.emergencyAccess = emergencyAccess; }
    
    public Map<String, String> getAttributes() { return attributes; }
    public void setAttributes(Map<String, String> attributes) { this.attributes = attributes; }
    
    public Object getContextValue(String key) {
        return context.get(key);
    }
    
    public String getAttribute(String key) {
        return attributes.get(key);
    }
    
    @Override
    public String toString() {
        return "AuthorizationRequest{" +
                "userId='" + userId + '\'' +
                ", action='" + action + '\'' +
                ", resourceId='" + resourceId + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", emergencyAccess=" + emergencyAccess +
                '}';
    }
    
    /**
     * Builder class for AuthorizationRequest
     */
    public static class AuthorizationRequestBuilder {
        private AuthorizationRequest request = new AuthorizationRequest();
        
        public AuthorizationRequestBuilder userId(String userId) {
            request.userId = userId;
            return this;
        }
        
        public AuthorizationRequestBuilder action(String action) {
            request.action = action;
            return this;
        }
        
        public AuthorizationRequestBuilder resourceId(String resourceId) {
            request.resourceId = resourceId;
            return this;
        }
        
        public AuthorizationRequestBuilder resourceType(String resourceType) {
            request.resourceType = resourceType;
            return this;
        }
        
        public AuthorizationRequestBuilder clientIp(String clientIp) {
            request.clientIp = clientIp;
            return this;
        }
        
        public AuthorizationRequestBuilder userAgent(String userAgent) {
            request.userAgent = userAgent;
            return this;
        }
        
        public AuthorizationRequestBuilder sessionId(String sessionId) {
            request.sessionId = sessionId;
            return this;
        }
        
        public AuthorizationRequestBuilder tenantId(String tenantId) {
            request.tenantId = tenantId;
            return this;
        }
        
        public AuthorizationRequestBuilder countryCode(String countryCode) {
            request.countryCode = countryCode;
            return this;
        }
        
        public AuthorizationRequestBuilder emergencyAccess(boolean emergencyAccess) {
            request.emergencyAccess = emergencyAccess;
            return this;
        }
        
        public AuthorizationRequestBuilder context(String key, Object value) {
            request.context.put(key, value);
            return this;
        }
        
        public AuthorizationRequestBuilder attribute(String key, String value) {
            request.attributes.put(key, value);
            return this;
        }
        
        public AuthorizationRequest build() {
            return request;
        }
    }
}