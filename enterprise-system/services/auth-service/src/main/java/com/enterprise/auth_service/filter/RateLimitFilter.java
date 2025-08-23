package com.enterprise.auth_service.filter;

import com.enterprise.auth_service.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Rate limiting filter for OAuth2 endpoints
 */
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter implements Filter {

    private final RateLimitService rateLimitService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestUri = httpRequest.getRequestURI();
        String clientId = extractClientId(httpRequest);
        String ipAddress = getClientIpAddress(httpRequest);
        
        boolean allowed = true;
        
        try {
            // Apply rate limiting based on endpoint
            if (requestUri.contains("/oauth2/token")) {
                allowed = rateLimitService.isTokenRequestAllowed(clientId, ipAddress);
            } else if (requestUri.contains("/oauth2/authorize")) {
                allowed = rateLimitService.isApiRequestAllowed(clientId, ipAddress, "authorize");
            } else if (requestUri.contains("/oauth2/introspect")) {
                allowed = rateLimitService.isIntrospectionAllowed(clientId, ipAddress);
            } else if (requestUri.contains("/oauth2/device_authorization")) {
                allowed = rateLimitService.isDeviceAuthorizationAllowed(clientId, ipAddress);
            } else if (requestUri.startsWith("/api/")) {
                allowed = rateLimitService.isApiRequestAllowed(clientId, ipAddress, requestUri);
            }
            
            if (!allowed) {
                httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write(
                    "{\"error\":\"rate_limit_exceeded\",\"error_description\":\"Too many requests\"}"
                );
                return;
            }
            
        } catch (Exception e) {
            log.error("Error in rate limit filter", e);
            // Continue processing if rate limit check fails
        }
        
        chain.doFilter(request, response);
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
        
        // Default to IP address if no client ID found
        return "unknown";
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