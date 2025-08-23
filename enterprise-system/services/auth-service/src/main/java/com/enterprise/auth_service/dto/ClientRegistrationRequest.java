package com.enterprise.auth_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.*;
import java.util.Set;

/**
 * DTO for OAuth2 client registration requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientRegistrationRequest {

    @NotBlank(message = "Client name is required")
    @Size(min = 3, max = 200, message = "Client name must be between 3 and 200 characters")
    private String clientName;

    @NotEmpty(message = "At least one redirect URI is required")
    private Set<@NotBlank @Pattern(regexp = "^https?://.*", message = "Invalid redirect URI format") String> redirectUris;

    @NotEmpty(message = "At least one grant type is required")
    private Set<@NotBlank String> authorizedGrantTypes;

    @NotEmpty(message = "At least one scope is required")
    private Set<@NotBlank String> scopes;

    @Min(value = 300, message = "Access token validity must be at least 300 seconds")
    @Max(value = 86400, message = "Access token validity must not exceed 24 hours")
    private Integer accessTokenValiditySeconds;

    @Min(value = 3600, message = "Refresh token validity must be at least 1 hour")
    @Max(value = 2592000, message = "Refresh token validity must not exceed 30 days")
    private Integer refreshTokenValiditySeconds;

    private Boolean requireAuthorizationConsent;

    private Boolean requireProofKey;

    private Set<String> clientAuthenticationMethods;

    @Min(value = 60, message = "Authorization code TTL must be at least 60 seconds")
    @Max(value = 600, message = "Authorization code TTL must not exceed 10 minutes")
    private Integer authorizationCodeTimeToLive;

    @Min(value = 300, message = "Device code TTL must be at least 300 seconds")
    @Max(value = 1800, message = "Device code TTL must not exceed 30 minutes")
    private Integer deviceCodeTimeToLive;

    @Min(value = 10, message = "Rate limit must be at least 10 requests per hour")
    @Max(value = 10000, message = "Rate limit must not exceed 10000 requests per hour")
    private Integer rateLimitPerHour;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Email(message = "Invalid email format")
    private String contactEmail;

    @Pattern(regexp = "^https?://.*", message = "Invalid website URL format")
    private String websiteUrl;
}