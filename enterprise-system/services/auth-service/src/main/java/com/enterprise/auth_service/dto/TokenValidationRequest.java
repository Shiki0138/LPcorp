package com.enterprise.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.Set;

/**
 * Request DTO for token validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenValidationRequest {

    @NotBlank(message = "Token is required")
    private String token;

    private Set<String> requiredScopes;

    private String expectedAudience;

    private String expectedIssuer;

    private boolean validateExpiration = true;

    private boolean validateSignature = true;

    private boolean validateAudience = true;

    private boolean validateIssuer = true;

    private boolean checkBlacklist = true;

    // For introspection
    private boolean includeTokenInfo = false;
}