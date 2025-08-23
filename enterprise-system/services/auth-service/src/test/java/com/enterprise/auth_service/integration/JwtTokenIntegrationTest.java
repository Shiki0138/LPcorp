package com.enterprise.auth_service.integration;

import com.enterprise.auth_service.dto.TokenRequest;
import com.enterprise.auth_service.dto.TokenResponse;
import com.enterprise.auth_service.entity.JwtToken;
import com.enterprise.auth_service.entity.RsaKeyPair;
import com.enterprise.auth_service.repository.JwtTokenRepository;
import com.enterprise.auth_service.repository.RsaKeyPairRepository;
import com.enterprise.auth_service.service.JwtTokenService;
import com.enterprise.auth_service.service.RsaKeyManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for JWT Token Management
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class JwtTokenIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:6-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresql::getJdbcUrl);
        registry.add("spring.datasource.username", postgresql::getUsername);
        registry.add("spring.datasource.password", postgresql::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private RsaKeyManagementService keyManagementService;

    @Autowired
    private JwtTokenRepository jwtTokenRepository;

    @Autowired
    private RsaKeyPairRepository rsaKeyPairRepository;

    private TokenRequest testTokenRequest;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        jwtTokenRepository.deleteAll();
        rsaKeyPairRepository.deleteAll();

        // Generate a test key pair
        RsaKeyPair keyPair = keyManagementService.generateKeyPair();
        keyManagementService.activateKeyPair(keyPair.getKeyId());

        // Set up test token request
        testTokenRequest = TokenRequest.builder()
                .userId("integration-test-user")
                .clientId("integration-test-client")
                .scopes(Set.of("read", "write", "admin"))
                .audience("integration-test-api")
                .subject("integration-test-user")
                .email("test@example.com")
                .name("Integration Test User")
                .includeIdToken(true)
                .build();
    }

    @Test
    void fullTokenLifecycle_ShouldWorkEndToEnd() {
        // 1. Generate tokens
        TokenResponse tokenResponse = jwtTokenService.generateTokens(testTokenRequest);
        
        assertNotNull(tokenResponse);
        assertNotNull(tokenResponse.getAccessToken());
        assertNotNull(tokenResponse.getRefreshToken());
        assertNotNull(tokenResponse.getIdToken());
        assertEquals("Bearer", tokenResponse.getTokenType());
        assertEquals(Long.valueOf(900), tokenResponse.getExpiresIn());

        // 2. Verify tokens are stored in database
        List<JwtToken> storedTokens = jwtTokenRepository.findByUserIdAndRevokedFalse("integration-test-user");
        assertEquals(3, storedTokens.size()); // Access, Refresh, ID tokens

        // Verify token types
        boolean hasAccessToken = storedTokens.stream()
                .anyMatch(token -> token.getTokenType() == JwtToken.TokenType.ACCESS_TOKEN);
        boolean hasRefreshToken = storedTokens.stream()
                .anyMatch(token -> token.getTokenType() == JwtToken.TokenType.REFRESH_TOKEN);
        boolean hasIdToken = storedTokens.stream()
                .anyMatch(token -> token.getTokenType() == JwtToken.TokenType.ID_TOKEN);

        assertTrue(hasAccessToken);
        assertTrue(hasRefreshToken);
        assertTrue(hasIdToken);

        // 3. Validate access token
        var validationRequest = com.enterprise.auth_service.dto.TokenValidationRequest.builder()
                .token(tokenResponse.getAccessToken())
                .includeTokenInfo(true)
                .build();

        var validationResponse = jwtTokenService.validateToken(validationRequest);
        assertTrue(validationResponse.isValid());
        assertEquals("integration-test-user", validationResponse.getUserId());
        assertEquals("integration-test-client", validationResponse.getClientId());

        // 4. Refresh tokens
        var refreshRequest = com.enterprise.auth_service.dto.RefreshTokenRequest.builder()
                .refreshToken(tokenResponse.getRefreshToken())
                .build();

        TokenResponse refreshedTokens = jwtTokenService.refreshTokens(refreshRequest);
        assertNotNull(refreshedTokens);
        assertNotNull(refreshedTokens.getAccessToken());
        assertNotNull(refreshedTokens.getRefreshToken());
        
        // New tokens should be different
        assertNotEquals(tokenResponse.getAccessToken(), refreshedTokens.getAccessToken());
        assertNotEquals(tokenResponse.getRefreshToken(), refreshedTokens.getRefreshToken());

        // 5. Revoke all user tokens
        jwtTokenService.revokeAllUserTokens("integration-test-user", "Integration test cleanup");

        // 6. Verify tokens are revoked
        List<JwtToken> revokedTokens = jwtTokenRepository.findByUserIdAndRevokedFalse("integration-test-user");
        assertTrue(revokedTokens.isEmpty());

        // 7. Verify revoked token validation fails
        var revokedValidationRequest = com.enterprise.auth_service.dto.TokenValidationRequest.builder()
                .token(tokenResponse.getAccessToken())
                .build();

        var revokedValidationResponse = jwtTokenService.validateToken(revokedValidationRequest);
        assertFalse(revokedValidationResponse.isValid());
        assertEquals("token_revoked", revokedValidationResponse.getError());
    }

    @Test
    void keyRotation_ShouldMaintainTokenValidity() {
        // 1. Generate tokens with current key
        TokenResponse initialTokens = jwtTokenService.generateTokens(testTokenRequest);
        
        // 2. Rotate keys
        RsaKeyPair newKeyPair = keyManagementService.generateKeyPair();
        keyManagementService.activateKeyPair(newKeyPair.getKeyId());

        // 3. Generate new tokens with new key
        testTokenRequest.setUserId("integration-test-user-2");
        TokenResponse newTokens = jwtTokenService.generateTokens(testTokenRequest);

        // 4. Both tokens should still be valid (old key should remain active)
        var oldTokenValidation = com.enterprise.auth_service.dto.TokenValidationRequest.builder()
                .token(initialTokens.getAccessToken())
                .build();

        var newTokenValidation = com.enterprise.auth_service.dto.TokenValidationRequest.builder()
                .token(newTokens.getAccessToken())
                .build();

        var oldTokenResponse = jwtTokenService.validateToken(oldTokenValidation);
        var newTokenResponse = jwtTokenService.validateToken(newTokenValidation);

        assertTrue(oldTokenResponse.isValid());
        assertTrue(newTokenResponse.isValid());
    }

    @Test
    void tokenWithCustomClaims_ShouldIncludeCustomData() {
        // 1. Add custom claims to token request
        testTokenRequest.getAdditionalClaims().put("department", "Engineering");
        testTokenRequest.getAdditionalClaims().put("level", 5);
        testTokenRequest.setRoles(Set.of("DEVELOPER", "TEAM_LEAD"));

        // 2. Generate tokens
        TokenResponse tokenResponse = jwtTokenService.generateTokens(testTokenRequest);

        // 3. Validate and check custom claims
        var validationRequest = com.enterprise.auth_service.dto.TokenValidationRequest.builder()
                .token(tokenResponse.getAccessToken())
                .includeTokenInfo(true)
                .build();

        var validationResponse = jwtTokenService.validateToken(validationRequest);
        assertTrue(validationResponse.isValid());
        
        // Custom claims should be available in additional claims
        assertNotNull(validationResponse.getAdditionalClaims());
    }

    @Test
    void concurrentTokenGeneration_ShouldNotCauseConflicts() {
        // Test concurrent token generation to ensure thread safety
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        TokenResponse[] responses = new TokenResponse[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                TokenRequest request = TokenRequest.builder()
                        .userId("concurrent-user-" + index)
                        .clientId("concurrent-client-" + index)
                        .scopes(Set.of("read"))
                        .build();
                
                responses[index] = jwtTokenService.generateTokens(request);
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Thread interrupted");
            }
        }

        // Verify all tokens were generated successfully
        for (int i = 0; i < threadCount; i++) {
            assertNotNull(responses[i], "Token response " + i + " should not be null");
            assertNotNull(responses[i].getAccessToken(), "Access token " + i + " should not be null");
        }

        // Verify all tokens are unique
        Set<String> uniqueTokens = Set.of(responses).stream()
                .map(TokenResponse::getAccessToken)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(threadCount, uniqueTokens.size(), "All tokens should be unique");
    }
}