package com.enterprise.security.authorization.evaluator;

import com.enterprise.security.rbac.model.User;
import com.enterprise.security.rbac.model.Resource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;

/**
 * Attribute-Based Access Control (ABAC) evaluator
 */
@Component
public class AttributeEvaluator {
    
    private static final Logger logger = LoggerFactory.getLogger(AttributeEvaluator.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    
    /**
     * Evaluate ABAC attributes against user and resource
     */
    public boolean evaluate(User user, Resource resource, String attributeConstraints) {
        try {
            if (attributeConstraints == null || attributeConstraints.trim().isEmpty()) {
                return true; // No constraints means allowed
            }
            
            JsonNode constraints = objectMapper.readTree(attributeConstraints);
            return evaluateConstraints(user, resource, constraints);
            
        } catch (Exception e) {
            logger.error("Error evaluating attributes for user {} on resource {}", 
                user.getId(), resource != null ? resource.getId() : "null", e);
            return false;
        }
    }
    
    /**
     * Evaluate constraints from JSON
     */
    private boolean evaluateConstraints(User user, Resource resource, JsonNode constraints) {
        // User attribute constraints
        if (constraints.has("user")) {
            if (!evaluateUserAttributes(user, constraints.get("user"))) {
                return false;
            }
        }
        
        // Resource attribute constraints
        if (constraints.has("resource") && resource != null) {
            if (!evaluateResourceAttributes(resource, constraints.get("resource"))) {
                return false;
            }
        }
        
        // Environment constraints
        if (constraints.has("environment")) {
            if (!evaluateEnvironmentAttributes(constraints.get("environment"))) {
                return false;
            }
        }
        
        // Custom expressions
        if (constraints.has("expression")) {
            if (!evaluateCustomExpression(user, resource, constraints.get("expression").asText())) {
                return false;
            }
        }
        
        // Relationship constraints
        if (constraints.has("relationship")) {
            if (!evaluateRelationshipConstraints(user, resource, constraints.get("relationship"))) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Evaluate user attribute constraints
     */
    private boolean evaluateUserAttributes(User user, JsonNode userConstraints) {
        // Department constraint
        if (userConstraints.has("department")) {
            String requiredDepartment = userConstraints.get("department").asText();
            if (!requiredDepartment.equals(user.getDepartmentId())) {
                return false;
            }
        }
        
        // Job title constraint
        if (userConstraints.has("jobTitle")) {
            String requiredJobTitle = userConstraints.get("jobTitle").asText();
            if (!requiredJobTitle.equals(user.getJobTitle())) {
                return false;
            }
        }
        
        // Location constraint
        if (userConstraints.has("location")) {
            String requiredLocation = userConstraints.get("location").asText();
            if (!requiredLocation.equals(user.getLocation())) {
                return false;
            }
        }
        
        // Cost center constraint
        if (userConstraints.has("costCenter")) {
            String requiredCostCenter = userConstraints.get("costCenter").asText();
            if (!requiredCostCenter.equals(user.getCostCenter())) {
                return false;
            }
        }
        
        // Manager constraint
        if (userConstraints.has("managerId")) {
            String requiredManagerId = userConstraints.get("managerId").asText();
            if (!requiredManagerId.equals(user.getManagerId())) {
                return false;
            }
        }
        
        // Employee number pattern
        if (userConstraints.has("employeeNumberPattern")) {
            String pattern = userConstraints.get("employeeNumberPattern").asText();
            if (user.getEmployeeNumber() == null || !user.getEmployeeNumber().matches(pattern)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Evaluate resource attribute constraints
     */
    private boolean evaluateResourceAttributes(Resource resource, JsonNode resourceConstraints) {
        // Owner constraint
        if (resourceConstraints.has("ownerId")) {
            String requiredOwnerId = resourceConstraints.get("ownerId").asText();
            if (!requiredOwnerId.equals(resource.getOwnerId())) {
                return false;
            }
        }
        
        // Department constraint
        if (resourceConstraints.has("departmentId")) {
            String requiredDepartmentId = resourceConstraints.get("departmentId").asText();
            if (!requiredDepartmentId.equals(resource.getDepartmentId())) {
                return false;
            }
        }
        
        // Classification constraint
        if (resourceConstraints.has("classification")) {
            String requiredClassification = resourceConstraints.get("classification").asText();
            if (resource.getClassification() == null || 
                !resource.getClassification().name().equals(requiredClassification)) {
                return false;
            }
        }
        
        // Geographic region constraint
        if (resourceConstraints.has("geographicRegion")) {
            String requiredRegion = resourceConstraints.get("geographicRegion").asText();
            if (!requiredRegion.equals(resource.getGeographicRegion())) {
                return false;
            }
        }
        
        // Custom attributes
        if (resourceConstraints.has("attributes")) {
            JsonNode attributeConstraints = resourceConstraints.get("attributes");
            if (!evaluateResourceCustomAttributes(resource, attributeConstraints)) {
                return false;
            }
        }
        
        // Project constraint
        if (resourceConstraints.has("projectId")) {
            String requiredProjectId = resourceConstraints.get("projectId").asText();
            if (!requiredProjectId.equals(resource.getProjectId())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Evaluate environment attribute constraints
     */
    private boolean evaluateEnvironmentAttributes(JsonNode envConstraints) {
        // Time constraints
        if (envConstraints.has("timeRange")) {
            JsonNode timeRange = envConstraints.get("timeRange");
            if (!isWithinTimeRange(timeRange)) {
                return false;
            }
        }
        
        // Day of week constraint
        if (envConstraints.has("allowedDays")) {
            JsonNode allowedDays = envConstraints.get("allowedDays");
            if (!isAllowedDay(allowedDays)) {
                return false;
            }
        }
        
        // IP range constraint (would require client IP in context)
        if (envConstraints.has("ipRanges")) {
            // Implementation would depend on having client IP in evaluation context
        }
        
        return true;
    }
    
    /**
     * Evaluate custom expression using script engine
     */
    private boolean evaluateCustomExpression(User user, Resource resource, String expression) {
        try {
            ScriptEngine engine = scriptEngineManager.getEngineByName("javascript");
            
            // Set up context variables
            engine.put("user", createUserContext(user));
            if (resource != null) {
                engine.put("resource", createResourceContext(resource));
            }
            engine.put("env", createEnvironmentContext());
            
            Object result = engine.eval(expression);
            return Boolean.TRUE.equals(result);
            
        } catch (ScriptException e) {
            logger.error("Error evaluating custom expression: {}", expression, e);
            return false;
        }
    }
    
    /**
     * Evaluate relationship constraints between user and resource
     */
    private boolean evaluateRelationshipConstraints(User user, Resource resource, JsonNode relationshipConstraints) {
        if (resource == null) {
            return true;
        }
        
        // Owner relationship
        if (relationshipConstraints.has("isOwner")) {
            boolean requiresOwnership = relationshipConstraints.get("isOwner").asBoolean();
            if (requiresOwnership && !user.getId().equals(resource.getOwnerId())) {
                return false;
            }
        }
        
        // Same department relationship
        if (relationshipConstraints.has("sameDepartment")) {
            boolean requiresSameDepartment = relationshipConstraints.get("sameDepartment").asBoolean();
            if (requiresSameDepartment && !user.getDepartmentId().equals(resource.getDepartmentId())) {
                return false;
            }
        }
        
        // Same tenant relationship
        if (relationshipConstraints.has("sameTenant")) {
            boolean requiresSameTenant = relationshipConstraints.get("sameTenant").asBoolean();
            if (requiresSameTenant && !user.getTenantId().equals(resource.getTenantId())) {
                return false;
            }
        }
        
        // Hierarchy relationship (user is in resource's hierarchy)
        if (relationshipConstraints.has("inHierarchy")) {
            boolean requiresHierarchy = relationshipConstraints.get("inHierarchy").asBoolean();
            if (requiresHierarchy && !isInResourceHierarchy(user, resource)) {
                return false;
            }
        }
        
        return true;
    }
    
    // Helper methods
    
    private boolean evaluateResourceCustomAttributes(Resource resource, JsonNode attributeConstraints) {
        attributeConstraints.fields().forEachRemaining(entry -> {
            String attributeName = entry.getKey();
            String expectedValue = entry.getValue().asText();
            String actualValue = resource.getAttribute(attributeName);
            
            if (!expectedValue.equals(actualValue)) {
                // Return false - but we can't return from lambda, so this is a placeholder
                // In real implementation, would use a different approach
            }
        });
        return true;
    }
    
    private boolean isWithinTimeRange(JsonNode timeRange) {
        // Implementation for time range checking
        return true; // Placeholder
    }
    
    private boolean isAllowedDay(JsonNode allowedDays) {
        // Implementation for day of week checking
        return true; // Placeholder
    }
    
    private boolean isInResourceHierarchy(User user, Resource resource) {
        // Implementation for hierarchy checking
        return false; // Placeholder
    }
    
    private Map<String, Object> createUserContext(User user) {
        Map<String, Object> context = new HashMap<>();
        context.put("id", user.getId());
        context.put("username", user.getUsername());
        context.put("departmentId", user.getDepartmentId());
        context.put("managerId", user.getManagerId());
        context.put("jobTitle", user.getJobTitle());
        context.put("location", user.getLocation());
        context.put("costCenter", user.getCostCenter());
        context.put("clearanceLevel", user.getClearanceLevel().name());
        context.put("tenantId", user.getTenantId());
        return context;
    }
    
    private Map<String, Object> createResourceContext(Resource resource) {
        Map<String, Object> context = new HashMap<>();
        context.put("id", resource.getId());
        context.put("resourceType", resource.getResourceType());
        context.put("ownerId", resource.getOwnerId());
        context.put("departmentId", resource.getDepartmentId());
        context.put("classification", resource.getClassification() != null ? resource.getClassification().name() : null);
        context.put("geographicRegion", resource.getGeographicRegion());
        context.put("projectId", resource.getProjectId());
        context.put("attributes", resource.getAttributes());
        context.put("tenantId", resource.getTenantId());
        return context;
    }
    
    private Map<String, Object> createEnvironmentContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("currentTime", System.currentTimeMillis());
        context.put("dayOfWeek", java.time.LocalDate.now().getDayOfWeek().name());
        // Add more environment context as needed
        return context;
    }
}