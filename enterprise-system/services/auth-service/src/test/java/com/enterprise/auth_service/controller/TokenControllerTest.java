package com.enterprise.auth_service.controller;

import com.enterprise.auth_service.dto.*;
import com.enterprise.auth_service.service.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for Token Controller
 */
@WebMvcTest(TokenController.class)
class TokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenService jwtTokenService;

    @Autowired
    private ObjectMapper objectMapper;

    private TokenRequest testTokenRequest;
    private TokenResponse testTokenResponse;

    @BeforeEach
    void setUp() {
        testTokenRequest = TokenRequest.builder()
                .userId("test-user-123")
                .clientId("test-client")
                .scopes(Set.of("read", "write"))
                .build();

        testTokenResponse = TokenResponse.builder()
                .accessToken("eyJhbGciOiJSUzI1NiJ9.test.access.token")
                .refreshToken("eyJhbGciOiJSUzI1NiJ9.test.refresh.token")
                .tokenType("Bearer")
                .expiresIn(900L)
                .scopes(Set.of("read", "write"))
                .issuedAt(Instant.now())
                .build();
    }

    @Test
    @WithMockUser(authorities = {"SCOPE_token:generate"})
    void generateTokens_ValidRequest_ShouldReturnTokenResponse() throws Exception {
        // Given
        when(jwtTokenService.generateTokens(any(TokenRequest.class)))
                .thenReturn(testTokenResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/tokens/generate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testTokenRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value(testTokenResponse.getAccessToken()))
                .andExpect(jsonPath("$.refreshToken").value(testTokenResponse.getRefreshToken()))
                .andExpected(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    @WithMockUser(authorities = {"SCOPE_token:generate"})
    void generateTokens_InvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given - Invalid request with missing required fields
        TokenRequest invalidRequest = TokenRequest.builder().build();

        // When & Then
        mockMvc.perform(post("/api/v1/tokens/generate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generateTokens_WithoutAuthorization_ShouldReturnUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/tokens/generate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testTokenRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"SCOPE_token:validate"})
    void validateToken_ValidRequest_ShouldReturnValidationResponse() throws Exception {
        // Given
        TokenValidationRequest validationRequest = TokenValidationRequest.builder()
                .token("valid.jwt.token")
                .build();

        TokenValidationResponse validationResponse = TokenValidationResponse.builder()
                .valid(true)
                .active(true)
                .userId("test-user-123")
                .clientId("test-client")
                .scopes(Set.of("read", "write"))
                .build();

        when(jwtTokenService.validateToken(any(TokenValidationRequest.class)))
                .thenReturn(validationResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/tokens/validate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.userId").value("test-user-123"));
    }

    @Test
    void refreshTokens_ValidRequest_ShouldReturnNewTokens() throws Exception {
        // Given
        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken("valid.refresh.token")
                .build();

        when(jwtTokenService.refreshTokens(any(RefreshTokenRequest.class)))
                .thenReturn(testTokenResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/tokens/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(testTokenResponse.getAccessToken()))
                .andExpected(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @WithMockUser(authorities = {"SCOPE_token:revoke"})
    void revokeToken_ValidRequest_ShouldReturnOk() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/tokens/revoke")
                        .with(csrf())
                        .param("tokenId", "test-token-123")
                        .param("reason", "Test revocation"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"SCOPE_token:revoke"})
    void revokeUserTokens_ValidRequest_ShouldReturnOk() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/tokens/revoke-user")
                        .with(csrf())
                        .param("userId", "test-user-123")
                        .param("reason", "User logout"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"SCOPE_token:introspect"})
    void introspectToken_ValidRequest_ShouldReturnTokenInfo() throws Exception {
        // Given
        TokenValidationResponse introspectionResponse = TokenValidationResponse.builder()
                .valid(true)
                .active(true)
                .userId("test-user-123")
                .clientId("test-client")
                .scopes(Set.of("read", "write"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build();

        when(jwtTokenService.introspectToken(any(String.class)))
                .thenReturn(introspectionResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/tokens/introspect")
                        .with(csrf())
                        .param("token", "valid.jwt.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.userId").value("test-user-123"));
    }

    @Test
    @WithMockUser(authorities = {"SCOPE_token:read"})
    void getTokenInfo_WithValidAuthHeader_ShouldReturnTokenInfo() throws Exception {
        // Given
        TokenValidationResponse tokenInfo = TokenValidationResponse.builder()
                .valid(true)
                .active(true)
                .userId("test-user-123")
                .clientId("test-client")
                .scopes(Set.of("read", "write"))
                .build();

        when(jwtTokenService.validateToken(any(TokenValidationRequest.class)))
                .thenReturn(tokenInfo);

        // When & Then
        mockMvc.perform(get("/api/v1/tokens/info")
                        .header("Authorization", "Bearer valid.jwt.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.userId").value("test-user-123"));
    }

    @Test
    @WithMockUser(authorities = {"SCOPE_token:read"})
    void getTokenInfo_WithoutAuthHeader_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/tokens/info"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("missing_token"));
    }

    @Test
    void health_ShouldReturnOk() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/tokens/health"))
                .andExpect(status().isOk())
                .andExpected(content().string("Token service is healthy"));
    }
}