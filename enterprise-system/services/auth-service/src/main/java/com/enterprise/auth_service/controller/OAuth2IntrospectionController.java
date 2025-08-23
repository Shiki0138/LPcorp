package com.enterprise.auth_service.controller;

import com.enterprise.auth_service.service.RateLimitService;
import com.enterprise.auth_service.service.SecurityEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2 Token Introspection Controller (RFC 7662)
 */
@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
@Slf4j
public class OAuth2IntrospectionController {

    private final OAuth2AuthorizationService authorizationService;
    private final RegisteredClientRepository registeredClientRepository;
    private final RateLimitService rateLimitService;
    private final SecurityEventService securityEventService;

    @PostMapping(value = "/introspect", 
                consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> introspectToken(
            @RequestParam("token") String token,
            @RequestParam(value = "token_type_hint", required = false) String tokenTypeHint,
            @RequestHeader("Authorization") String authorization,
            HttpServletRequest request) {

        String clientId = extractClientId(authorization);
        String ipAddress = getClientIpAddress(request);

        // Rate limiting
        if (!rateLimitService.isIntrospectionAllowed(clientId, ipAddress)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(createErrorResponse("rate_limit_exceeded", "Too many requests"));
        }

        // Validate client
        RegisteredClient client = registeredClientRepository.findByClientId(clientId);
        if (client == null) {
            securityEventService.logUnauthorizedAccess(null, clientId, "introspection", 
                ipAddress, request.getHeader("User-Agent"), "Invalid client");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(createInactiveTokenResponse());
        }

        try {
            OAuth2TokenType tokenType = determineTokenType(tokenTypeHint);
            OAuth2Authorization authorization1 = authorizationService.findByToken(token, tokenType);

            if (authorization1 == null) {
                securityEventService.logTokenIntrospection(clientId, "unknown", ipAddress, false);
                return ResponseEntity.ok(createInactiveTokenResponse());
            }

            // Check if token belongs to the requesting client or if client has introspection rights
            if (!authorization1.getRegisteredClientId().equals(clientId) && !hasIntrospectionRights(client)) {
                securityEventService.logUnauthorizedAccess(null, clientId, "introspection", 
                    ipAddress, request.getHeader("User-Agent"), "Insufficient privileges");
                return ResponseEntity.ok(createInactiveTokenResponse());
            }

            Map<String, Object> response = createIntrospectionResponse(authorization1, token, tokenType);
            securityEventService.logTokenIntrospection(clientId, 
                tokenType != null ? tokenType.getValue() : "unknown", ipAddress, true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during token introspection", e);
            securityEventService.logTokenIntrospection(clientId, "error", ipAddress, false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("server_error", "Internal server error"));
        }
    }

    private Map<String, Object> createIntrospectionResponse(OAuth2Authorization authorization, 
                                                           String token, OAuth2TokenType tokenType) {
        Map<String, Object> response = new HashMap<>();
        
        boolean active = false;
        Instant expiresAt = null;
        Instant issuedAt = null;
        String scope = null;
        String username = authorization.getPrincipalName();
        String clientId = authorization.getRegisteredClientId();

        // Check access token
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = 
            authorization.getToken(OAuth2AccessToken.class);
        if (accessToken != null && token.equals(accessToken.getToken().getTokenValue())) {
            active = accessToken.getToken().getExpiresAt() != null && 
                    accessToken.getToken().getExpiresAt().isAfter(Instant.now());
            expiresAt = accessToken.getToken().getExpiresAt();
            issuedAt = accessToken.getToken().getIssuedAt();
            if (accessToken.getToken().getScopes() != null) {
                scope = String.join(" ", accessToken.getToken().getScopes());
            }
        }

        // Check refresh token
        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = 
            authorization.getToken(OAuth2RefreshToken.class);
        if (refreshToken != null && token.equals(refreshToken.getToken().getTokenValue())) {
            active = refreshToken.getToken().getExpiresAt() != null && 
                    refreshToken.getToken().getExpiresAt().isAfter(Instant.now());
            expiresAt = refreshToken.getToken().getExpiresAt();
            issuedAt = refreshToken.getToken().getIssuedAt();
        }

        response.put("active", active);
        
        if (active) {
            response.put("client_id", clientId);
            response.put("username", username);
            response.put("token_type", "Bearer");
            
            if (scope != null) {
                response.put("scope", scope);
            }
            
            if (expiresAt != null) {
                response.put("exp", expiresAt.getEpochSecond());
            }
            
            if (issuedAt != null) {
                response.put("iat", issuedAt.getEpochSecond());
            }
            
            response.put("sub", username);
            response.put("aud", clientId);
        }

        return response;
    }

    private Map<String, Object> createInactiveTokenResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("active", false);
        return response;
    }

    private Map<String, Object> createErrorResponse(String error, String description) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", error);
        response.put("error_description", description);
        return response;
    }

    private OAuth2TokenType determineTokenType(String tokenTypeHint) {
        if ("access_token".equals(tokenTypeHint)) {
            return OAuth2TokenType.ACCESS_TOKEN;
        } else if ("refresh_token".equals(tokenTypeHint)) {
            return OAuth2TokenType.REFRESH_TOKEN;
        }
        return null; // Will search all token types
    }

    private String extractClientId(String authorization) {
        if (authorization != null && authorization.startsWith("Basic ")) {
            try {
                String credentials = new String(
                    java.util.Base64.getDecoder().decode(authorization.substring(6))
                );
                return credentials.split(":")[0];
            } catch (Exception e) {
                log.warn("Failed to extract client ID from authorization header", e);
            }
        }
        return null;
    }

    private boolean hasIntrospectionRights(RegisteredClient client) {
        // In a real implementation, you might check specific scopes or client permissions
        // For now, we'll allow introspection only for the token owner
        return false;
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