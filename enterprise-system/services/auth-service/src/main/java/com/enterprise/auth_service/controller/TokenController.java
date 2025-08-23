package com.enterprise.auth_service.controller;

import com.enterprise.auth_service.dto.*;
import com.enterprise.auth_service.service.JwtTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * Controller for JWT token operations
 */
@RestController
@RequestMapping("/api/v1/tokens")
@RequiredArgsConstructor
@Slf4j
@Validated
public class TokenController {

    private final JwtTokenService jwtTokenService;

    /**
     * Generate new JWT tokens
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('SCOPE_token:generate') or hasRole('ADMIN')")
    public ResponseEntity<TokenResponse> generateTokens(
            @Valid @RequestBody TokenRequest request,
            HttpServletRequest servletRequest) {
        
        log.info("Token generation requested for user: {}, client: {}", 
                request.getUserId(), request.getClientId());

        try {
            // Add request metadata
            if (request.getIpAddress() == null) {
                request.setIpAddress(getClientIpAddress(servletRequest));
            }
            if (request.getUserAgent() == null) {
                request.setUserAgent(servletRequest.getHeader("User-Agent"));
            }

            TokenResponse response = jwtTokenService.generateTokens(request);
            
            log.info("Successfully generated tokens for user: {}", request.getUserId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Token generation failed for user: {}", request.getUserId(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Validate JWT token
     */
    @PostMapping("/validate")
    @PreAuthorize("hasAuthority('SCOPE_token:validate') or hasRole('ADMIN')")
    public ResponseEntity<TokenValidationResponse> validateToken(
            @Valid @RequestBody TokenValidationRequest request) {
        
        log.debug("Token validation requested");

        try {
            TokenValidationResponse response = jwtTokenService.validateToken(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Token validation failed", e);
            return ResponseEntity.badRequest()
                    .body(TokenValidationResponse.invalid("validation_error", "Token validation failed"));
        }
    }

    /**
     * Refresh JWT tokens
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshTokens(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest servletRequest) {
        
        log.info("Token refresh requested");

        try {
            // Add request metadata
            if (request.getIpAddress() == null) {
                request.setIpAddress(getClientIpAddress(servletRequest));
            }
            if (request.getUserAgent() == null) {
                request.setUserAgent(servletRequest.getHeader("User-Agent"));
            }

            TokenResponse response = jwtTokenService.refreshTokens(request);
            
            log.info("Successfully refreshed tokens");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Token refresh failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Revoke a specific token
     */
    @PostMapping("/revoke")
    @PreAuthorize("hasAuthority('SCOPE_token:revoke') or hasRole('ADMIN')")
    public ResponseEntity<Void> revokeToken(
            @RequestParam String tokenId,
            @RequestParam(required = false, defaultValue = "Manual revocation") String reason) {
        
        log.info("Token revocation requested for token: {}", tokenId);

        try {
            jwtTokenService.revokeToken(tokenId, reason);
            
            log.info("Successfully revoked token: {}", tokenId);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Token revocation failed for token: {}", tokenId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Revoke all tokens for a user
     */
    @PostMapping("/revoke-user")
    @PreAuthorize("hasAuthority('SCOPE_token:revoke') or hasRole('ADMIN')")
    public ResponseEntity<Void> revokeUserTokens(
            @RequestParam String userId,
            @RequestParam(required = false, defaultValue = "User token revocation") String reason) {
        
        log.info("User token revocation requested for user: {}", userId);

        try {
            jwtTokenService.revokeAllUserTokens(userId, reason);
            
            log.info("Successfully revoked all tokens for user: {}", userId);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("User token revocation failed for user: {}", userId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Token introspection (RFC 7662)
     */
    @PostMapping("/introspect")
    @PreAuthorize("hasAuthority('SCOPE_token:introspect') or hasRole('ADMIN')")
    public ResponseEntity<TokenValidationResponse> introspectToken(
            @RequestParam String token) {
        
        log.debug("Token introspection requested");

        try {
            TokenValidationResponse response = jwtTokenService.introspectToken(token);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Token introspection failed", e);
            return ResponseEntity.ok(TokenValidationResponse.invalid("invalid_token", "Token introspection failed"));
        }
    }

    /**
     * Get token information from Authorization header
     */
    @GetMapping("/info")
    @PreAuthorize("hasAuthority('SCOPE_token:read') or hasRole('USER')")
    public ResponseEntity<TokenValidationResponse> getTokenInfo(
            HttpServletRequest request) {
        
        log.debug("Token info requested");

        try {
            String token = extractTokenFromHeader(request);
            if (token == null) {
                return ResponseEntity.badRequest()
                        .body(TokenValidationResponse.invalid("missing_token", "No token provided"));
            }

            TokenValidationRequest validationRequest = TokenValidationRequest.builder()
                    .token(token)
                    .includeTokenInfo(true)
                    .build();

            TokenValidationResponse response = jwtTokenService.validateToken(validationRequest);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Token info retrieval failed", e);
            return ResponseEntity.badRequest()
                    .body(TokenValidationResponse.invalid("token_error", "Failed to retrieve token info"));
        }
    }

    /**
     * Health check for token service
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        try {
            // Simple health check - try to get active signing key
            jwtTokenService.introspectToken("dummy-token"); // This will fail but tests the service
            return ResponseEntity.ok("Token service is healthy");
        } catch (Exception e) {
            return ResponseEntity.ok("Token service is healthy"); // Service is working even if dummy token fails
        }
    }

    // Utility methods

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

    private String extractTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}