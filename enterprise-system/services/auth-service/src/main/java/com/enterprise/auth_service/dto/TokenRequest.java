package com.enterprise.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.Set;

/**
 * Request DTO for token generation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Client ID is required")
    private String clientId;

    @NotEmpty(message = "At least one scope is required")
    private Set<String> scopes;

    private String audience;

    private String subject;

    private Long accessTokenExpirationSeconds;

    private Long refreshTokenExpirationSeconds;

    private String ipAddress;

    private String userAgent;

    // Additional claims for custom tokens
    private java.util.Map<String, Object> additionalClaims;

    // For service-to-service authentication
    private boolean serviceToken = false;

    // For ID tokens (OpenID Connect)
    private boolean includeIdToken = false;

    // User information for ID token
    private String email;
    private String name;
    private String picture;
    private Set<String> roles;
}