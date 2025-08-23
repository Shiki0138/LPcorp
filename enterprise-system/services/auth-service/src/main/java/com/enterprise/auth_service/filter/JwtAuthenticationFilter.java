package com.enterprise.auth_service.filter;

import com.enterprise.auth_service.dto.TokenValidationRequest;
import com.enterprise.auth_service.dto.TokenValidationResponse;
import com.enterprise.auth_service.service.JwtTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter for validating JWT tokens in requests
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String X_API_KEY_HEADER = "X-API-Key";

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String token = extractToken(request);
            
            if (token != null) {
                TokenValidationResponse validationResponse = validateToken(token, request);
                
                if (validationResponse.isValid()) {
                    setAuthenticationContext(validationResponse, request);
                } else {
                    log.debug("Token validation failed: {}", validationResponse.getError());
                }
            }
            
        } catch (Exception e) {
            log.error("JWT authentication filter error", e);
        }
        
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip filter for public endpoints
        return path.startsWith("/.well-known/") ||
               path.startsWith("/actuator/") ||
               path.startsWith("/api/v1/tokens/health") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/");
    }

    private String extractToken(HttpServletRequest request) {
        // Try Authorization header first
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        
        // Try X-API-Key header for service-to-service communication
        String apiKey = request.getHeader(X_API_KEY_HEADER);
        if (apiKey != null) {
            return apiKey;
        }
        
        return null;
    }

    private TokenValidationResponse validateToken(String token, HttpServletRequest request) {
        TokenValidationRequest validationRequest = TokenValidationRequest.builder()
                .token(token)
                .validateExpiration(true)
                .validateSignature(true)
                .validateAudience(true)
                .validateIssuer(true)
                .checkBlacklist(true)
                .includeTokenInfo(true)
                .build();

        return jwtTokenService.validateToken(validationRequest);
    }

    private void setAuthenticationContext(TokenValidationResponse validationResponse, 
                                        HttpServletRequest request) {
        
        String userId = validationResponse.getUserId();
        Set<String> scopes = validationResponse.getScopes();
        
        // Convert scopes to authorities
        List<SimpleGrantedAuthority> authorities = scopes.stream()
                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                .collect(Collectors.toList());
        
        // Add role-based authorities if available
        if (validationResponse.getAdditionalClaims() != null) {
            Object roles = validationResponse.getAdditionalClaims().get("roles");
            if (roles instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> roleList = (List<String>) roles;
                
                roleList.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                        .forEach(authorities::add);
            }
        }

        // Create authentication object
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                userId,
                validationResponse,
                authorities
        );
        
        // Add request details
        authentication.setDetails(createAuthenticationDetails(validationResponse, request));
        
        // Set in security context
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        log.debug("JWT authentication successful for user: {}", userId);
    }

    private JwtAuthenticationDetails createAuthenticationDetails(TokenValidationResponse validationResponse,
                                                               HttpServletRequest request) {
        return JwtAuthenticationDetails.builder()
                .tokenId(validationResponse.getJwtId())
                .clientId(validationResponse.getClientId())
                .tokenType(validationResponse.getTokenType())
                .issuedAt(validationResponse.getIssuedAt())
                .expiresAt(validationResponse.getExpiresAt())
                .scopes(validationResponse.getScopes())
                .ipAddress(getClientIpAddress(request))
                .userAgent(request.getHeader("User-Agent"))
                .requestUri(request.getRequestURI())
                .requestMethod(request.getMethod())
                .build();
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