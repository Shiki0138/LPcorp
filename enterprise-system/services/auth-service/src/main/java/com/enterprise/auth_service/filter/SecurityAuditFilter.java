package com.enterprise.auth_service.filter;

import com.enterprise.auth_service.service.SecurityEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Security audit filter for logging security events
 */
@RequiredArgsConstructor
@Slf4j
public class SecurityAuditFilter implements Filter {

    private final SecurityEventService securityEventService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestUri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        String clientId = extractClientId(httpRequest);
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        long startTime = System.currentTimeMillis();
        
        try {
            chain.doFilter(request, response);
        } finally {
            long endTime = System.currentTimeMillis();
            int statusCode = httpResponse.getStatus();
            
            // Log security events for OAuth2 endpoints
            logSecurityEvent(requestUri, method, clientId, ipAddress, userAgent, 
                            statusCode, endTime - startTime);
        }
    }

    private void logSecurityEvent(String requestUri, String method, String clientId, 
                                 String ipAddress, String userAgent, int statusCode, long duration) {
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth != null ? auth.getName() : null;
            
            boolean isSuccess = statusCode >= 200 && statusCode < 400;
            
            if (requestUri.contains("/oauth2/token")) {
                if (isSuccess) {
                    securityEventService.logTokenGeneration(username, clientId, "access_token", ipAddress);
                } else {
                    securityEventService.logLoginFailure(username, clientId, ipAddress, userAgent, 
                        "TOKEN_REQUEST_FAILED", "Token request failed with status: " + statusCode);
                }
            } else if (requestUri.contains("/oauth2/authorize")) {
                String eventType = isSuccess ? "AUTHORIZATION_SUCCESS" : "AUTHORIZATION_FAILURE";
                logSecurityEvent(eventType, username, clientId, ipAddress, userAgent, isSuccess, 
                    requestUri, statusCode + "");
            } else if (requestUri.contains("/oauth2/introspect")) {
                securityEventService.logTokenIntrospection(clientId, "access_token", ipAddress, isSuccess);
            } else if (requestUri.startsWith("/api/oauth2/clients")) {
                String eventType = determineClientManagementEvent(method, requestUri, isSuccess);
                if (eventType != null) {
                    logSecurityEvent(eventType, username, clientId, ipAddress, userAgent, isSuccess, 
                        requestUri, null);
                }
            }
            
            // Log suspicious activity for certain patterns
            if (duration > 10000) { // Requests taking more than 10 seconds
                securityEventService.logSuspiciousActivity(username, "SLOW_REQUEST", ipAddress, 
                    userAgent, java.util.Map.of("uri", requestUri, "duration", duration));
            }
            
            if (statusCode == 401 || statusCode == 403) {
                securityEventService.logUnauthorizedAccess(username, clientId, requestUri, 
                    ipAddress, userAgent, "HTTP " + statusCode);
            }
            
        } catch (Exception e) {
            log.error("Error logging security event", e);
        }
    }

    private void logSecurityEvent(String eventType, String username, String clientId, 
                                 String ipAddress, String userAgent, boolean success, 
                                 String resource, String errorCode) {
        // Implementation would call SecurityEventService with appropriate parameters
        // This is a simplified version
        log.info("Security event: {} for user: {} client: {} success: {}", 
                eventType, username, clientId, success);
    }

    private String determineClientManagementEvent(String method, String requestUri, boolean isSuccess) {
        if (!isSuccess) {
            return null; // Don't log failed client management attempts
        }
        
        if (method.equals("POST") && !requestUri.contains("/")) {
            return "CLIENT_CREATED";
        } else if (method.equals("PUT")) {
            return "CLIENT_UPDATED";
        } else if (method.equals("DELETE")) {
            return "CLIENT_DELETED";
        } else if (requestUri.contains("/regenerate-secret")) {
            return "CLIENT_SECRET_REGENERATED";
        } else if (requestUri.contains("/activate")) {
            return "CLIENT_ACTIVATED";
        } else if (requestUri.contains("/deactivate")) {
            return "CLIENT_DEACTIVATED";
        }
        
        return null;
    }

    private String extractClientId(HttpServletRequest request) {
        // Try to extract from Authorization header (Basic auth)
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Basic ")) {
            try {
                String credentials = new String(
                    java.util.Base64.getDecoder().decode(authorization.substring(6))
                );
                return credentials.split(":")[0];
            } catch (Exception e) {
                // Ignore and try other methods
            }
        }
        
        // Try to extract from client_id parameter
        String clientId = request.getParameter("client_id");
        if (clientId != null) {
            return clientId;
        }
        
        return null;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}