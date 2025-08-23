package com.enterprise.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Response DTO for token validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenValidationResponse {

    private boolean valid;
    private String error;
    private String errorDescription;

    // Token information (for introspection)
    private boolean active;
    private String userId;
    private String clientId;
    private Set<String> scopes;
    private String audience;
    private String issuer;
    private String subject;
    private Instant issuedAt;
    private Instant expiresAt;
    private Instant notBefore;
    private String tokenType;
    private String jwtId;

    // Additional claims
    private Map<String, Object> additionalClaims;

    public static TokenValidationResponse valid() {
        return TokenValidationResponse.builder()
                .valid(true)
                .active(true)
                .build();
    }

    public static TokenValidationResponse invalid(String error, String errorDescription) {
        return TokenValidationResponse.builder()
                .valid(false)
                .active(false)
                .error(error)
                .errorDescription(errorDescription)
                .build();
    }

    public static TokenValidationResponse expired() {
        return invalid("token_expired", "The token has expired");
    }

    public static TokenValidationResponse revoked() {
        return invalid("token_revoked", "The token has been revoked");
    }

    public static TokenValidationResponse malformed() {
        return invalid("invalid_token", "The token is malformed or invalid");
    }

    public static TokenValidationResponse insufficientScope() {
        return invalid("insufficient_scope", "The token does not have the required scope");
    }
}