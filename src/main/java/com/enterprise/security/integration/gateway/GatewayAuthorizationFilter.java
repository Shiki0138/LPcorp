package com.enterprise.security.integration.gateway;

import com.enterprise.security.authorization.engine.RbacAuthorizationEngine;
import com.enterprise.security.authorization.engine.AuthorizationRequest;
import com.enterprise.security.authorization.engine.AuthorizationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Gateway filter for API-level authorization using RBAC
 */
@Component
public class GatewayAuthorizationFilter implements GatewayFilter, Ordered {
    
    private static final Logger logger = LoggerFactory.getLogger(GatewayAuthorizationFilter.class);
    
    @Autowired
    private RbacAuthorizationEngine authorizationEngine;
    
    // Route to resource type mapping
    private final Map<Pattern, String> routeResourceMapping = new ConcurrentHashMap<>();
    
    // Route to action mapping
    private final Map<Pattern, String> routeActionMapping = new ConcurrentHashMap<>();
    
    public GatewayAuthorizationFilter() {
        initializeRouteMappings();
    }
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();
        
        // Skip authorization for public endpoints
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }
        
        // Skip authorization for health checks and monitoring
        if (isInternalEndpoint(path)) {
            return chain.filter(exchange);
        }
        
        return ReactiveSecurityContextHolder.getContext()
            .cast(org.springframework.security.core.context.SecurityContext.class)
            .map(context -> context.getAuthentication())
            .filter(Authentication::isAuthenticated)
            .cast(Authentication.class)
            .flatMap(auth -> {
                String userId = extractUserId(auth);
                if (userId == null) {
                    return handleUnauthorized(exchange, "User not authenticated");
                }
                
                // Determine resource type and action from route
                String resourceType = determineResourceType(path);
                String action = determineAction(method, path);
                String resourceId = extractResourceId(path);
                
                // Create authorization request
                AuthorizationRequest authRequest = AuthorizationRequest.builder()
                    .userId(userId)
                    .action(action)
                    .resourceId(resourceId)
                    .resourceType(resourceType)
                    .clientIp(getClientIp(request))
                    .userAgent(request.getHeaders().getFirst("User-Agent"))
                    .build();
                
                // Perform authorization check
                try {
                    AuthorizationResult result = authorizationEngine.authorize(authRequest);
                    
                    if (result.isGranted()) {
                        // Add authorization context to request headers
                        ServerHttpRequest modifiedRequest = request.mutate()
                            .header("X-User-Id", userId)
                            .header("X-Resource-Type", resourceType)
                            .header("X-Action", action)
                            .header("X-Permission-Source", result.getPermissionSource())
                            .build();
                        
                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
                    } else {
                        logger.warn("Access denied for user {} on {} {}: {}", 
                                  userId, method, path, result.getReason());
                        return handleForbidden(exchange, result.getReason());
                    }
                } catch (Exception e) {
                    logger.error("Authorization error for user {} on {} {}", 
                               userId, method, path, e);
                    return handleInternalError(exchange);
                }
            })
            .switchIfEmpty(handleUnauthorized(exchange, "No authentication context"));
    }
    
    @Override
    public int getOrder() {
        return -100; // Execute after authentication but before other filters
    }
    
    /**
     * Initialize route to resource/action mappings
     */
    private void initializeRouteMappings() {
        // API resource mappings
        routeResourceMapping.put(Pattern.compile("/api/users.*"), "user");
        routeResourceMapping.put(Pattern.compile("/api/roles.*"), "role");
        routeResourceMapping.put(Pattern.compile("/api/permissions.*"), "permission");
        routeResourceMapping.put(Pattern.compile("/api/resources.*"), "resource");
        routeResourceMapping.put(Pattern.compile("/api/documents.*"), "document");
        routeResourceMapping.put(Pattern.compile("/api/reports.*"), "report");
        routeResourceMapping.put(Pattern.compile("/api/projects.*"), "project");
        routeResourceMapping.put(Pattern.compile("/api/admin.*"), "admin");
        
        // Action mappings based on HTTP method and path patterns
        routeActionMapping.put(Pattern.compile("GET:/api/.*/export"), "export");
        routeActionMapping.put(Pattern.compile("POST:/api/.*/import"), "import");
        routeActionMapping.put(Pattern.compile("DELETE:/api/.*"), "delete");
        routeActionMapping.put(Pattern.compile("PUT:/api/.*"), "update");
        routeActionMapping.put(Pattern.compile("PATCH:/api/.*"), "update");
        routeActionMapping.put(Pattern.compile("POST:/api/.*"), "create");
        routeActionMapping.put(Pattern.compile("GET:/api/.*"), "read");
    }
    
    /**
     * Determine resource type from request path
     */
    private String determineResourceType(String path) {
        for (Map.Entry<Pattern, String> entry : routeResourceMapping.entrySet()) {
            if (entry.getKey().matcher(path).matches()) {
                return entry.getValue();
            }
        }
        
        // Default extraction from path segments
        String[] segments = path.split("/");
        if (segments.length >= 3 && "api".equals(segments[1])) {
            return segments[2]; // /api/{resource-type}/...
        }
        
        return "unknown";
    }
    
    /**
     * Determine action from HTTP method and path
     */
    private String determineAction(String method, String path) {
        String methodPath = method + ":" + path;
        
        for (Map.Entry<Pattern, String> entry : routeActionMapping.entrySet()) {
            if (entry.getKey().matcher(methodPath).matches()) {
                return entry.getValue();
            }
        }
        
        // Default mapping based on HTTP method
        switch (method.toUpperCase()) {
            case "GET": return "read";
            case "POST": return "create";
            case "PUT": case "PATCH": return "update";
            case "DELETE": return "delete";
            default: return "access";
        }
    }
    
    /**
     * Extract resource ID from path (if present)
     */
    private String extractResourceId(String path) {
        // Common patterns for resource IDs
        String[] segments = path.split("/");
        
        // Look for UUID-like segments
        for (String segment : segments) {
            if (segment.matches("[0-9a-fA-F-]{36}") || segment.matches("\\d+")) {
                return segment;
            }
        }
        
        return null;
    }
    
    /**
     * Extract user ID from authentication
     */
    private String extractUserId(Authentication auth) {
        if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            return ((org.springframework.security.core.userdetails.UserDetails) auth.getPrincipal()).getUsername();
        } else if (auth.getPrincipal() instanceof String) {
            return (String) auth.getPrincipal();
        }
        return null;
    }
    
    /**
     * Get client IP address
     */
    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddress() != null ? 
               request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }
    
    /**
     * Check if endpoint is public (no authorization required)
     */
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/public/") ||
               path.startsWith("/auth/") ||
               path.equals("/health") ||
               path.equals("/info") ||
               path.startsWith("/swagger") ||
               path.startsWith("/v3/api-docs");
    }
    
    /**
     * Check if endpoint is internal (monitoring, etc.)
     */
    private boolean isInternalEndpoint(String path) {
        return path.startsWith("/actuator/") ||
               path.startsWith("/metrics") ||
               path.startsWith("/prometheus");
    }
    
    /**
     * Handle unauthorized access
     */
    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String reason) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        
        String body = String.format("{\"
    + "error\": \"Unauthorized\", \"
    + "message\": \"%s\", \"
    + "timestamp\": \"%s\"}", reason, java.time.Instant.now().toString());
        
        org.springframework.core.io.buffer.DataBuffer buffer = 
            response.bufferFactory().wrap(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        
        return response.writeWith(Mono.just(buffer));
    }
    
    /**
     * Handle forbidden access
     */
    private Mono<Void> handleForbidden(ServerWebExchange exchange, String reason) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add("Content-Type", "application/json");
        
        String body = String.format("{\"
    + "error\": \"Forbidden\", \"
    + "message\": \"%s\", \"
    + "timestamp\": \"%s\"}", reason, java.time.Instant.now().toString());
        
        org.springframework.core.io.buffer.DataBuffer buffer = 
            response.bufferFactory().wrap(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        
        return response.writeWith(Mono.just(buffer));
    }
    
    /**
     * Handle internal server error
     */
    private Mono<Void> handleInternalError(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        response.getHeaders().add("Content-Type", "application/json");
        
        String body = String.format("{\"
    + "error\": \"Internal Server Error\", \"
    + "message\": \"Authorization service error\", \"
    + "timestamp\": \"%s\"}", java.time.Instant.now().toString());
        
        org.springframework.core.io.buffer.DataBuffer buffer = 
            response.bufferFactory().wrap(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        
        return response.writeWith(Mono.just(buffer));
    }
}