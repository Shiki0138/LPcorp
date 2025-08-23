package com.enterprise.auth_service.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

/**
 * Authentication details for JWT tokens
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtAuthenticationDetails {

    private String tokenId;
    private String clientId;
    private String tokenType;
    private Instant issuedAt;
    private Instant expiresAt;
    private Set<String> scopes;
    private String ipAddress;
    private String userAgent;
    private String requestUri;
    private String requestMethod;
}