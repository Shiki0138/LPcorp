package com.enterprise.auth_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.Set;

/**
 * DTO for OAuth2 client registration responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientRegistrationResponse {

    private String clientId;

    private String clientSecret; // Only included in response, never in subsequent requests

    private String clientName;

    private Set<String> redirectUris;

    private Set<String> authorizedGrantTypes;

    private Set<String> scopes;

    private Integer accessTokenValiditySeconds;

    private Integer refreshTokenValiditySeconds;

    private Boolean requireAuthorizationConsent;

    private Boolean requireProofKey;

    private Set<String> clientAuthenticationMethods;

    private Integer authorizationCodeTimeToLive;

    private Integer deviceCodeTimeToLive;

    private Boolean isActive;

    private Integer rateLimitPerHour;

    private Instant createdAt;

    private Instant updatedAt;

    private String createdBy;

    private String description;

    private String contactEmail;

    private String websiteUrl;

    // Metadata for client management
    private Instant lastUsedAt;

    private Long totalTokensIssued;

    private Boolean secretRegenerated;
}