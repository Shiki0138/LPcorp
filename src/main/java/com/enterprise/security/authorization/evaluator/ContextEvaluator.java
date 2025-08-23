package com.enterprise.security.authorization.evaluator;

import com.enterprise.security.rbac.model.User;
import com.enterprise.security.rbac.model.Resource;
import com.enterprise.security.authorization.engine.AuthorizationRequest;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.Map;

/**
 * Context-aware evaluator using Spring Expression Language (SpEL)
 */
@Component
public class ContextEvaluator {
    
    private static final Logger logger = LoggerFactory.getLogger(ContextEvaluator.class);
    
    private final ExpressionParser parser = new SpelExpressionParser();
    
    /**
     * Evaluate a condition expression using SpEL
     */
    public boolean evaluateCondition(User user, Resource resource, AuthorizationRequest request, String conditionExpression) {
        try {
            if (conditionExpression == null || conditionExpression.trim().isEmpty()) {
                return true;
            }
            
            StandardEvaluationContext context = createEvaluationContext(user, resource, request);
            Expression expression = parser.parseExpression(conditionExpression);
            
            Object result = expression.getValue(context);
            return Boolean.TRUE.equals(result);
            
        } catch (Exception e) {
            logger.error("Error evaluating condition expression: {}", conditionExpression, e);
            return false;
        }
    }
    
    /**
     * Create evaluation context with all available variables
     */
    private StandardEvaluationContext createEvaluationContext(User user, Resource resource, AuthorizationRequest request) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        // User context
        if (user != null) {
            context.setVariable("user", createUserSpelContext(user));
        }
        
        // Resource context
        if (resource != null) {
            context.setVariable("resource", createResourceSpelContext(resource));
        }
        
        // Request context
        if (request != null) {
            context.setVariable("request", createRequestSpelContext(request));
        }
        
        // Environment context
        context.setVariable("env", createEnvironmentSpelContext());
        
        // Time context
        context.setVariable("time", createTimeSpelContext());
        
        // Register custom functions
        registerCustomFunctions(context);
        
        return context;
    }
    
    /**
     * Create user context for SpEL evaluation
     */
    private UserSpelContext createUserSpelContext(User user) {
        return new UserSpelContext(user);
    }
    
    /**
     * Create resource context for SpEL evaluation
     */
    private ResourceSpelContext createResourceSpelContext(Resource resource) {
        return new ResourceSpelContext(resource);
    }
    
    /**
     * Create request context for SpEL evaluation
     */
    private RequestSpelContext createRequestSpelContext(AuthorizationRequest request) {
        return new RequestSpelContext(request);
    }
    
    /**
     * Create environment context for SpEL evaluation
     */
    private Map<String, Object> createEnvironmentSpelContext() {
        Map<String, Object> env = new HashMap<>();
        env.put("serverTime", System.currentTimeMillis());
        env.put("hostname", getHostname());
        env.put("environment", getEnvironment());
        return env;
    }
    
    /**
     * Create time context for SpEL evaluation
     */
    private TimeSpelContext createTimeSpelContext() {
        return new TimeSpelContext();
    }
    
    /**
     * Register custom functions for SpEL
     */
    private void registerCustomFunctions(StandardEvaluationContext context) {
        try {
            // Register utility functions
            context.registerFunction("isWeekday", 
                ContextEvaluator.class.getDeclaredMethod("isWeekday"));
            context.registerFunction("isBusinessHours", 
                ContextEvaluator.class.getDeclaredMethod("isBusinessHours"));
            context.registerFunction("isWithinTimeRange", 
                ContextEvaluator.class.getDeclaredMethod("isWithinTimeRange", LocalTime.class, LocalTime.class));
            context.registerFunction("hasRole", 
                ContextEvaluator.class.getDeclaredMethod("hasRole", User.class, String.class));
            context.registerFunction("inDepartment", 
                ContextEvaluator.class.getDeclaredMethod("inDepartment", User.class, String.class));
            
        } catch (NoSuchMethodException e) {
            logger.warn("Could not register custom functions for SpEL context", e);
        }
    }
    
    // Custom SpEL functions
    
    public static boolean isWeekday() {
        DayOfWeek today = LocalDateTime.now().getDayOfWeek();
        return today != DayOfWeek.SATURDAY && today != DayOfWeek.SUNDAY;
    }
    
    public static boolean isBusinessHours() {
        LocalTime now = LocalTime.now();
        return now.isAfter(LocalTime.of(9, 0)) && now.isBefore(LocalTime.of(17, 0));
    }
    
    public static boolean isWithinTimeRange(LocalTime start, LocalTime end) {
        LocalTime now = LocalTime.now();
        if (start.isBefore(end)) {
            return !now.isBefore(start) && !now.isAfter(end);
        } else {
            // Overnight range
            return !now.isBefore(start) || !now.isAfter(end);
        }
    }
    
    public static boolean hasRole(User user, String roleName) {
        return user.hasRole(roleName);
    }
    
    public static boolean inDepartment(User user, String departmentId) {
        return departmentId.equals(user.getDepartmentId());
    }
    
    // Helper methods
    
    private String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private String getEnvironment() {
        return System.getProperty("spring.profiles.active", "default");
    }
    
    /**
     * User context wrapper for SpEL
     */
    public static class UserSpelContext {
        private final User user;
        
        public UserSpelContext(User user) {
            this.user = user;
        }
        
        public String getId() { return user.getId(); }
        public String getUsername() { return user.getUsername(); }
        public String getEmail() { return user.getEmail(); }
        public String getDepartmentId() { return user.getDepartmentId(); }
        public String getManagerId() { return user.getManagerId(); }
        public String getJobTitle() { return user.getJobTitle(); }
        public String getLocation() { return user.getLocation(); }
        public String getTenantId() { return user.getTenantId(); }
        public String getClearanceLevel() { return user.getClearanceLevel().name(); }
        public boolean isActive() { return user.isActive(); }
        public boolean hasRole(String roleName) { return user.hasRole(roleName); }
    }
    
    /**
     * Resource context wrapper for SpEL
     */
    public static class ResourceSpelContext {
        private final Resource resource;
        
        public ResourceSpelContext(Resource resource) {
            this.resource = resource;
        }
        
        public String getId() { return resource.getId(); }
        public String getResourceType() { return resource.getResourceType(); }
        public String getOwnerId() { return resource.getOwnerId(); }
        public String getDepartmentId() { return resource.getDepartmentId(); }
        public String getTenantId() { return resource.getTenantId(); }
        public String getClassification() { 
            return resource.getClassification() != null ? resource.getClassification().name() : null; 
        }
        public String getGeographicRegion() { return resource.getGeographicRegion(); }
        public String getProjectId() { return resource.getProjectId(); }
        public boolean isActive() { return resource.isActive(); }
        public boolean hasAttribute(String name) { return resource.hasAttribute(name); }
        public String getAttribute(String name) { return resource.getAttribute(name); }
    }
    
    /**
     * Request context wrapper for SpEL
     */
    public static class RequestSpelContext {
        private final AuthorizationRequest request;
        
        public RequestSpelContext(AuthorizationRequest request) {
            this.request = request;
        }
        
        public String getUserId() { return request.getUserId(); }
        public String getAction() { return request.getAction(); }
        public String getResourceId() { return request.getResourceId(); }
        public String getResourceType() { return request.getResourceType(); }
        public String getClientIp() { return request.getClientIp(); }
        public String getUserAgent() { return request.getUserAgent(); }
        public String getSessionId() { return request.getSessionId(); }
        public String getTenantId() { return request.getTenantId(); }
        public String getCountryCode() { return request.getCountryCode(); }
        public boolean isEmergencyAccess() { return request.isEmergencyAccess(); }
        public Object getContextValue(String key) { return request.getContextValue(key); }
        public String getAttribute(String key) { return request.getAttribute(key); }
    }
    
    /**
     * Time context for SpEL
     */
    public static class TimeSpelContext {
        public LocalDateTime now() { return LocalDateTime.now(); }
        public LocalTime timeNow() { return LocalTime.now(); }
        public DayOfWeek dayOfWeek() { return LocalDateTime.now().getDayOfWeek(); }
        public int hour() { return LocalDateTime.now().getHour(); }
        public int minute() { return LocalDateTime.now().getMinute(); }
        public boolean isWeekend() { 
            DayOfWeek day = dayOfWeek();
            return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
        }
        public boolean isWeekday() { return !isWeekend(); }
    }
}