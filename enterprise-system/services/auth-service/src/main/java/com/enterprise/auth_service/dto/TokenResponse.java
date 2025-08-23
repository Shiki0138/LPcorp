package com.enterprise.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

/**
 * Response DTO for token operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private String idToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private Set<String> scopes;
    private String jwtId;
    private Instant issuedAt;
    private Instant expiresAt;

    public static TokenResponse of(String accessToken, String refreshToken, Long expiresIn, Set<String> scopes) {
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .scopes(scopes)
                .issuedAt(Instant.now())
                .build();
    }

    public static TokenResponse accessTokenOnly(String accessToken, Long expiresIn, Set<String> scopes) {
        return TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .scopes(scopes)
                .issuedAt(Instant.now())
                .build();
    }
}