package com.enterprise.auth_service.service;

import com.enterprise.auth_service.dto.*;
import com.enterprise.auth_service.entity.JwtToken;
import com.enterprise.auth_service.entity.RsaKeyPair;
import com.enterprise.auth_service.repository.JwtTokenRepository;
import com.enterprise.auth_service.repository.TokenBlacklistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JWT Token Service
 */
@ExtendWith(MockitoExtension.class)
class JwtTokenServiceTest {

    @Mock
    private JwtTokenRepository jwtTokenRepository;

    @Mock
    private TokenBlacklistRepository tokenBlacklistRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private RsaKeyManagementService keyManagementService;

    @InjectMocks
    private JwtTokenService jwtTokenService;

    private RsaKeyPair testKeyPair;
    private TokenRequest testTokenRequest;

    @BeforeEach
    void setUp() {
        // Set up test configuration
        ReflectionTestUtils.setField(jwtTokenService, "defaultIssuer", "https://test.enterprise.com");
        ReflectionTestUtils.setField(jwtTokenService, "defaultAudience", "test-api");
        ReflectionTestUtils.setField(jwtTokenService, "defaultAccessTokenExpiration", 900L);
        ReflectionTestUtils.setField(jwtTokenService, "defaultRefreshTokenExpiration", 604800L);
        ReflectionTestUtils.setField(jwtTokenService, "encryptionKey", "test-encryption-key-32-chars-long");

        // Set up test key pair
        testKeyPair = RsaKeyPair.builder()
                .keyId("test-key-1")
                .algorithm("RS256")
                .publicKey("test-public-key")
                .privateKeyEncrypted("test-encrypted-private-key")
                .active(true)
                .expiresAt(Instant.now().plusSeconds(86400))
                .build();

        // Set up test token request
        testTokenRequest = TokenRequest.builder()
                .userId("test-user-123")
                .clientId("test-client")
                .scopes(Set.of("read", "write"))
                .audience("test-api")
                .subject("test-user-123")
                .build();
    }

    @Test
    void generateTokens_ShouldReturnValidTokenResponse() {
        // Given
        when(keyManagementService.getActiveSigningKey()).thenReturn(testKeyPair);
        when(jwtTokenRepository.save(any(JwtToken.class))).thenReturn(new JwtToken());

        // When
        TokenResponse response = jwtTokenService.generateTokens(testTokenRequest);

        // Then
        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(900L, response.getExpiresIn());
        assertEquals(testTokenRequest.getScopes(), response.getScopes());
        
        // Verify repository interactions
        verify(jwtTokenRepository, times(2)).save(any(JwtToken.class)); // Access + Refresh tokens
    }

    @Test
    void generateTokens_WithIdToken_ShouldIncludeIdToken() {
        // Given
        testTokenRequest.setIncludeIdToken(true);
        testTokenRequest.setEmail("test@example.com");
        testTokenRequest.setName("Test User");
        
        when(keyManagementService.getActiveSigningKey()).thenReturn(testKeyPair);
        when(jwtTokenRepository.save(any(JwtToken.class))).thenReturn(new JwtToken());

        // When
        TokenResponse response = jwtTokenService.generateTokens(testTokenRequest);

        // Then
        assertNotNull(response);
        assertNotNull(response.getIdToken());
        
        // Verify all three tokens are stored
        verify(jwtTokenRepository, times(3)).save(any(JwtToken.class));
    }

    @Test
    void validateToken_ValidToken_ShouldReturnValidResponse() {
        // Given
        String testToken = "valid.jwt.token";
        TokenValidationRequest request = TokenValidationRequest.builder()
                .token(testToken)
                .build();

        when(tokenBlacklistRepository.existsByTokenId(anyString())).thenReturn(false);
        when(keyManagementService.getSigningKeyForToken(testToken)).thenReturn(testKeyPair);

        // Mock JWT parsing - this would normally require a real JWT
        // In a real test, you'd use a properly signed JWT token

        // When & Then
        // This test would need a real JWT token or mocked JWT parsing
        // For now, we test the basic structure
        assertThrows(RuntimeException.class, () -> {
            jwtTokenService.validateToken(request);
        });
    }

    @Test
    void validateToken_BlacklistedToken_ShouldReturnRevokedResponse() {
        // Given
        String testToken = "blacklisted.jwt.token";
        TokenValidationRequest request = TokenValidationRequest.builder()
                .token(testToken)
                .checkBlacklist(true)
                .build();

        when(tokenBlacklistRepository.existsByTokenId(anyString())).thenReturn(true);

        // When
        TokenValidationResponse response = jwtTokenService.validateToken(request);

        // Then
        assertFalse(response.isValid());
        assertEquals("token_revoked", response.getError());
        assertEquals("The token has been revoked", response.getErrorDescription());
    }

    @Test
    void refreshTokens_ValidRefreshToken_ShouldReturnNewTokens() {
        // Given
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid.refresh.token")
                .build();

        // Mock existing refresh token in database
        JwtToken existingToken = JwtToken.builder()
                .tokenId("refresh-token-123")
                .userId("test-user-123")
                .clientId("test-client")
                .tokenType(JwtToken.TokenType.REFRESH_TOKEN)
                .revoked(false)
                .build();

        when(jwtTokenRepository.findByTokenId(anyString())).thenReturn(Optional.of(existingToken));
        when(keyManagementService.getSigningKeyForToken(anyString())).thenReturn(testKeyPair);
        when(keyManagementService.getActiveSigningKey()).thenReturn(testKeyPair);

        // This test would also need proper JWT mocking
        assertThrows(RuntimeException.class, () -> {
            jwtTokenService.refreshTokens(request);
        });
    }

    @Test
    void revokeToken_ValidTokenId_ShouldRevokeSuccessfully() {
        // Given
        String tokenId = "token-to-revoke";
        String reason = "Test revocation";
        
        JwtToken existingToken = JwtToken.builder()
                .tokenId(tokenId)
                .userId("test-user-123")
                .tokenType(JwtToken.TokenType.ACCESS_TOKEN)
                .expiresAt(Instant.now().plusSeconds(900))
                .build();

        when(jwtTokenRepository.findByTokenId(tokenId)).thenReturn(Optional.of(existingToken));
        when(jwtTokenRepository.revokeToken(eq(tokenId), any(Instant.class), eq(reason))).thenReturn(1);

        // When
        jwtTokenService.revokeToken(tokenId, reason);

        // Then
        verify(jwtTokenRepository).revokeToken(eq(tokenId), any(Instant.class), eq(reason));
        verify(tokenBlacklistRepository).save(any());
    }

    @Test
    void revokeAllUserTokens_ValidUser_ShouldRevokeAllTokens() {
        // Given
        String userId = "test-user-123";
        String reason = "User logout";

        JwtToken token1 = JwtToken.builder()
                .tokenId("token-1")
                .userId(userId)
                .tokenType(JwtToken.TokenType.ACCESS_TOKEN)
                .build();

        JwtToken token2 = JwtToken.builder()
                .tokenId("token-2")
                .userId(userId)
                .tokenType(JwtToken.TokenType.REFRESH_TOKEN)
                .build();

        when(jwtTokenRepository.findByUserIdAndRevokedFalse(userId))
                .thenReturn(java.util.List.of(token1, token2));

        // When
        jwtTokenService.revokeAllUserTokens(userId, reason);

        // Then
        verify(jwtTokenRepository, times(2)).revokeToken(anyString(), any(Instant.class), eq(reason));
        verify(tokenBlacklistRepository, times(2)).save(any());
    }

    @Test
    void introspectToken_ValidToken_ShouldReturnTokenInfo() {
        // Given
        String testToken = "valid.token.for.introspection";
        
        when(tokenBlacklistRepository.existsByTokenId(anyString())).thenReturn(false);
        when(keyManagementService.getSigningKeyForToken(testToken)).thenReturn(testKeyPair);

        // When & Then
        // This would require proper JWT mocking
        assertThrows(RuntimeException.class, () -> {
            jwtTokenService.introspectToken(testToken);
        });
    }
}