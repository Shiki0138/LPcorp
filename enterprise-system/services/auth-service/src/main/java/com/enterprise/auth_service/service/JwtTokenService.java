package com.enterprise.auth_service.service;

import com.enterprise.auth_service.dto.*;
import com.enterprise.auth_service.entity.JwtToken;
import com.enterprise.auth_service.entity.RsaKeyPair;
import com.enterprise.auth_service.entity.TokenBlacklist;
import com.enterprise.auth_service.repository.JwtTokenRepository;
import com.enterprise.auth_service.repository.RsaKeyPairRepository;
import com.enterprise.auth_service.repository.TokenBlacklistRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service for JWT token generation, validation, and management
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class JwtTokenService {

    private final JwtTokenRepository jwtTokenRepository;
    private final RsaKeyPairRepository rsaKeyPairRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final RsaKeyManagementService keyManagementService;

    @Value("${jwt.issuer:https://auth.enterprise.com}")
    private String defaultIssuer;

    @Value("${jwt.audience:enterprise-api}")
    private String defaultAudience;

    @Value("${jwt.access-token.expiration:900}") // 15 minutes
    private long defaultAccessTokenExpiration;

    @Value("${jwt.refresh-token.expiration:604800}") // 7 days
    private long defaultRefreshTokenExpiration;

    @Value("${jwt.id-token.expiration:3600}") // 1 hour
    private long defaultIdTokenExpiration;

    @Value("${encryption.key}")
    private String encryptionKey;

    private static final String TOKEN_CACHE_PREFIX = "jwt:token:";
    private static final String BLACKLIST_CACHE_PREFIX = "jwt:blacklist:";

    /**
     * Generate JWT tokens (access, refresh, and optionally ID token)
     */
    public TokenResponse generateTokens(TokenRequest request) {
        log.debug("Generating tokens for user: {}, client: {}", request.getUserId(), request.getClientId());

        try {
            RsaKeyPair activeKey = keyManagementService.getActiveSigningKey();
            PrivateKey privateKey = decryptPrivateKey(activeKey.getPrivateKeyEncrypted());

            // Generate access token
            String accessToken = generateAccessToken(request, activeKey.getKeyId(), privateKey);
            
            // Generate refresh token
            String refreshToken = generateRefreshToken(request, activeKey.getKeyId(), privateKey);
            
            // Generate ID token if requested
            String idToken = null;
            if (request.isIncludeIdToken()) {
                idToken = generateIdToken(request, activeKey.getKeyId(), privateKey);
            }

            // Store tokens in database
            storeTokens(request, accessToken, refreshToken, idToken);

            // Cache tokens for fast lookup
            cacheTokens(accessToken, refreshToken, request.getUserId());

            TokenResponse response = TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .idToken(idToken)
                    .tokenType("Bearer")
                    .expiresIn(getAccessTokenExpiration(request))
                    .scopes(request.getScopes())
                    .issuedAt(Instant.now())
                    .build();

            log.info("Successfully generated tokens for user: {}", request.getUserId());
            return response;

        } catch (Exception e) {
            log.error("Failed to generate tokens for user: {}", request.getUserId(), e);
            throw new RuntimeException("Token generation failed", e);
        }
    }

    /**
     * Validate JWT token
     */
    public TokenValidationResponse validateToken(TokenValidationRequest request) {
        log.debug("Validating token");

        try {
            // Check blacklist first (from cache if available)
            if (request.isCheckBlacklist() && isTokenBlacklisted(request.getToken())) {
                return TokenValidationResponse.revoked();
            }

            // Parse and validate token
            Claims claims = parseToken(request.getToken());
            
            // Additional validations
            if (request.isValidateAudience() && !validateAudience(claims, request.getExpectedAudience())) {
                return TokenValidationResponse.invalid("invalid_audience", "Invalid audience");
            }

            if (request.isValidateIssuer() && !validateIssuer(claims, request.getExpectedIssuer())) {
                return TokenValidationResponse.invalid("invalid_issuer", "Invalid issuer");
            }

            if (request.getRequiredScopes() != null && !validateScopes(claims, request.getRequiredScopes())) {
                return TokenValidationResponse.insufficientScope();
            }

            // Update last used timestamp
            updateTokenUsage(getTokenId(claims));

            // Build response
            TokenValidationResponse response = TokenValidationResponse.valid();
            if (request.isIncludeTokenInfo()) {
                populateTokenInfo(response, claims);
            }

            log.debug("Token validation successful");
            return response;

        } catch (ExpiredJwtException e) {
            log.debug("Token expired: {}", e.getMessage());
            return TokenValidationResponse.expired();
        } catch (JwtException e) {
            log.debug("Invalid token: {}", e.getMessage());
            return TokenValidationResponse.malformed();
        } catch (Exception e) {
            log.error("Token validation failed", e);
            return TokenValidationResponse.invalid("validation_error", "Token validation failed");
        }
    }

    /**
     * Refresh tokens using refresh token
     */
    public TokenResponse refreshTokens(RefreshTokenRequest request) {
        log.debug("Refreshing tokens");

        try {
            // Validate refresh token
            Claims claims = parseToken(request.getRefreshToken());
            String tokenType = claims.get("token_type", String.class);
            
            if (!"refresh".equals(tokenType)) {
                throw new IllegalArgumentException("Invalid token type for refresh");
            }

            // Check if refresh token is still valid in database
            String tokenId = getTokenId(claims);
            Optional<JwtToken> storedToken = jwtTokenRepository.findByTokenId(tokenId);
            
            if (storedToken.isEmpty() || storedToken.get().isRevoked()) {
                throw new IllegalArgumentException("Refresh token is revoked");
            }

            // Create new token request from refresh token
            TokenRequest newTokenRequest = TokenRequest.builder()
                    .userId(claims.getSubject())
                    .clientId(claims.get("client_id", String.class))
                    .scopes(extractScopes(claims))
                    .audience(claims.getAudience().iterator().next())
                    .subject(claims.getSubject())
                    .ipAddress(request.getIpAddress())
                    .userAgent(request.getUserAgent())
                    .build();

            // Generate new tokens
            TokenResponse newTokens = generateTokens(newTokenRequest);

            // Revoke old refresh token
            revokeToken(tokenId, "Token refreshed");

            log.info("Successfully refreshed tokens for user: {}", claims.getSubject());
            return newTokens;

        } catch (Exception e) {
            log.error("Token refresh failed", e);
            throw new RuntimeException("Token refresh failed", e);
        }
    }

    /**
     * Revoke a specific token
     */
    public void revokeToken(String tokenId, String reason) {
        log.debug("Revoking token: {}", tokenId);

        try {
            // Update database
            jwtTokenRepository.revokeToken(tokenId, Instant.now(), reason);
            
            // Add to blacklist
            Optional<JwtToken> token = jwtTokenRepository.findByTokenId(tokenId);
            if (token.isPresent()) {
                TokenBlacklist blacklistEntry = TokenBlacklist.builder()
                        .tokenId(tokenId)
                        .userId(token.get().getUserId())
                        .tokenType(token.get().getTokenType())
                        .reason(reason)
                        .expiresAt(token.get().getExpiresAt())
                        .build();
                tokenBlacklistRepository.save(blacklistEntry);

                // Cache blacklist entry
                cacheBlacklistEntry(tokenId, blacklistEntry.getExpiresAt());
            }

            log.info("Successfully revoked token: {}", tokenId);

        } catch (Exception e) {
            log.error("Failed to revoke token: {}", tokenId, e);
            throw new RuntimeException("Token revocation failed", e);
        }
    }

    /**
     * Revoke all tokens for a user
     */
    public void revokeAllUserTokens(String userId, String reason) {
        log.debug("Revoking all tokens for user: {}", userId);

        try {
            List<JwtToken> userTokens = jwtTokenRepository.findByUserIdAndRevokedFalse(userId);
            
            for (JwtToken token : userTokens) {
                revokeToken(token.getTokenId(), reason);
            }

            log.info("Successfully revoked all tokens for user: {}", userId);

        } catch (Exception e) {
            log.error("Failed to revoke tokens for user: {}", userId, e);
            throw new RuntimeException("Token revocation failed", e);
        }
    }

    /**
     * Get token introspection information
     */
    public TokenValidationResponse introspectToken(String token) {
        TokenValidationRequest request = TokenValidationRequest.builder()
                .token(token)
                .includeTokenInfo(true)
                .build();
        
        return validateToken(request);
    }

    // Private helper methods

    private String generateAccessToken(TokenRequest request, String keyId, PrivateKey privateKey) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(getAccessTokenExpiration(request));
        String jti = UUID.randomUUID().toString();

        JwtBuilder builder = Jwts.builder()
                .setHeaderParam("kid", keyId)
                .setId(jti)
                .setIssuer(getIssuer(request))
                .setAudience(getAudience(request))
                .setSubject(request.getSubject() != null ? request.getSubject() : request.getUserId())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .claim("scope", String.join(" ", request.getScopes()))
                .claim("client_id", request.getClientId())
                .claim("user_id", request.getUserId())
                .claim("token_type", "access");

        // Add custom claims
        if (request.getAdditionalClaims() != null) {
            request.getAdditionalClaims().forEach(builder::claim);
        }

        return builder.signWith(privateKey, SignatureAlgorithm.RS256).compact();
    }

    private String generateRefreshToken(TokenRequest request, String keyId, PrivateKey privateKey) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(getRefreshTokenExpiration(request));
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .setHeaderParam("kid", keyId)
                .setId(jti)
                .setIssuer(getIssuer(request))
                .setAudience(getAudience(request))
                .setSubject(request.getSubject() != null ? request.getSubject() : request.getUserId())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .claim("scope", String.join(" ", request.getScopes()))
                .claim("client_id", request.getClientId())
                .claim("user_id", request.getUserId())
                .claim("token_type", "refresh")
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    private String generateIdToken(TokenRequest request, String keyId, PrivateKey privateKey) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(defaultIdTokenExpiration);
        String jti = UUID.randomUUID().toString();

        JwtBuilder builder = Jwts.builder()
                .setHeaderParam("kid", keyId)
                .setId(jti)
                .setIssuer(getIssuer(request))
                .setAudience(request.getClientId()) // ID token audience is the client
                .setSubject(request.getUserId())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .claim("token_type", "id");

        // Add OpenID Connect standard claims
        if (request.getEmail() != null) {
            builder.claim("email", request.getEmail());
        }
        if (request.getName() != null) {
            builder.claim("name", request.getName());
        }
        if (request.getPicture() != null) {
            builder.claim("picture", request.getPicture());
        }
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            builder.claim("roles", request.getRoles());
        }

        return builder.signWith(privateKey, SignatureAlgorithm.RS256).compact();
    }

    private Claims parseToken(String token) throws JwtException {
        RsaKeyPair signingKey = keyManagementService.getSigningKeyForToken(token);
        PublicKey publicKey = parsePublicKey(signingKey.getPublicKey());

        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private void storeTokens(TokenRequest request, String accessToken, String refreshToken, String idToken) {
        Instant now = Instant.now();

        // Store access token
        String accessTokenId = getTokenIdFromToken(accessToken);
        JwtToken accessTokenEntity = JwtToken.builder()
                .tokenId(accessTokenId)
                .userId(request.getUserId())
                .clientId(request.getClientId())
                .tokenType(JwtToken.TokenType.ACCESS_TOKEN)
                .tokenHash(hashToken(accessToken))
                .scopes(request.getScopes())
                .audience(getAudience(request))
                .issuer(getIssuer(request))
                .subject(request.getSubject() != null ? request.getSubject() : request.getUserId())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(getAccessTokenExpiration(request)))
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .build();
        jwtTokenRepository.save(accessTokenEntity);

        // Store refresh token
        String refreshTokenId = getTokenIdFromToken(refreshToken);
        JwtToken refreshTokenEntity = JwtToken.builder()
                .tokenId(refreshTokenId)
                .userId(request.getUserId())
                .clientId(request.getClientId())
                .tokenType(JwtToken.TokenType.REFRESH_TOKEN)
                .tokenHash(hashToken(refreshToken))
                .scopes(request.getScopes())
                .audience(getAudience(request))
                .issuer(getIssuer(request))
                .subject(request.getSubject() != null ? request.getSubject() : request.getUserId())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(getRefreshTokenExpiration(request)))
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .build();
        jwtTokenRepository.save(refreshTokenEntity);

        // Store ID token if present
        if (idToken != null) {
            String idTokenId = getTokenIdFromToken(idToken);
            JwtToken idTokenEntity = JwtToken.builder()
                    .tokenId(idTokenId)
                    .userId(request.getUserId())
                    .clientId(request.getClientId())
                    .tokenType(JwtToken.TokenType.ID_TOKEN)
                    .tokenHash(hashToken(idToken))
                    .scopes(request.getScopes())
                    .audience(request.getClientId())
                    .issuer(getIssuer(request))
                    .subject(request.getUserId())
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(defaultIdTokenExpiration))
                    .ipAddress(request.getIpAddress())
                    .userAgent(request.getUserAgent())
                    .build();
            jwtTokenRepository.save(idTokenEntity);
        }
    }

    private void cacheTokens(String accessToken, String refreshToken, String userId) {
        String accessTokenId = getTokenIdFromToken(accessToken);
        String refreshTokenId = getTokenIdFromToken(refreshToken);

        // Cache with appropriate TTL
        redisTemplate.opsForValue().set(
                TOKEN_CACHE_PREFIX + accessTokenId, 
                userId, 
                defaultAccessTokenExpiration, 
                TimeUnit.SECONDS
        );
        
        redisTemplate.opsForValue().set(
                TOKEN_CACHE_PREFIX + refreshTokenId, 
                userId, 
                defaultRefreshTokenExpiration, 
                TimeUnit.SECONDS
        );
    }

    private boolean isTokenBlacklisted(String token) {
        String tokenId = getTokenIdFromToken(token);
        
        // Check cache first
        String cacheKey = BLACKLIST_CACHE_PREFIX + tokenId;
        Boolean cached = redisTemplate.hasKey(cacheKey);
        if (Boolean.TRUE.equals(cached)) {
            return true;
        }

        // Check database
        return tokenBlacklistRepository.existsByTokenId(tokenId);
    }

    private void cacheBlacklistEntry(String tokenId, Instant expiresAt) {
        long ttl = ChronoUnit.SECONDS.between(Instant.now(), expiresAt);
        if (ttl > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_CACHE_PREFIX + tokenId, 
                    "revoked", 
                    ttl, 
                    TimeUnit.SECONDS
            );
        }
    }

    private void updateTokenUsage(String tokenId) {
        try {
            jwtTokenRepository.updateLastUsedAt(tokenId, Instant.now());
        } catch (Exception e) {
            log.warn("Failed to update token usage for: {}", tokenId, e);
        }
    }

    private String getTokenIdFromToken(String token) {
        try {
            String[] chunks = token.split("\\.");
            if (chunks.length >= 2) {
                String payload = new String(Base64.getDecoder().decode(chunks[1]));
                // Simple extraction - in production, use proper JSON parsing
                return extractJtiFromPayload(payload);
            }
        } catch (Exception e) {
            log.warn("Failed to extract token ID from token", e);
        }
        return UUID.randomUUID().toString(); // Fallback
    }

    private String extractJtiFromPayload(String payload) {
        // This is a simplified implementation
        // In production, use Jackson or other JSON library
        int jtiIndex = payload.indexOf("\"jti\":\"");
        if (jtiIndex != -1) {
            int start = jtiIndex + 7;
            int end = payload.indexOf("\"", start);
            if (end != -1) {
                return payload.substring(start, end);
            }
        }
        return UUID.randomUUID().toString();
    }

    // Utility methods
    private long getAccessTokenExpiration(TokenRequest request) {
        return request.getAccessTokenExpirationSeconds() != null ? 
                request.getAccessTokenExpirationSeconds() : defaultAccessTokenExpiration;
    }

    private long getRefreshTokenExpiration(TokenRequest request) {
        return request.getRefreshTokenExpirationSeconds() != null ? 
                request.getRefreshTokenExpirationSeconds() : defaultRefreshTokenExpiration;
    }

    private String getIssuer(TokenRequest request) {
        return defaultIssuer;
    }

    private String getAudience(TokenRequest request) {
        return request.getAudience() != null ? request.getAudience() : defaultAudience;
    }

    private String getTokenId(Claims claims) {
        return claims.getId();
    }

    private Set<String> extractScopes(Claims claims) {
        String scopeString = claims.get("scope", String.class);
        return scopeString != null ? 
                new HashSet<>(Arrays.asList(scopeString.split(" "))) : 
                new HashSet<>();
    }

    private boolean validateAudience(Claims claims, String expectedAudience) {
        if (expectedAudience == null) return true;
        return claims.getAudience().contains(expectedAudience);
    }

    private boolean validateIssuer(Claims claims, String expectedIssuer) {
        if (expectedIssuer == null) return true;
        return expectedIssuer.equals(claims.getIssuer());
    }

    private boolean validateScopes(Claims claims, Set<String> requiredScopes) {
        Set<String> tokenScopes = extractScopes(claims);
        return tokenScopes.containsAll(requiredScopes);
    }

    private void populateTokenInfo(TokenValidationResponse response, Claims claims) {
        response.setUserId(claims.get("user_id", String.class));
        response.setClientId(claims.get("client_id", String.class));
        response.setScopes(extractScopes(claims));
        response.setAudience(String.join(",", claims.getAudience()));
        response.setIssuer(claims.getIssuer());
        response.setSubject(claims.getSubject());
        response.setIssuedAt(claims.getIssuedAt().toInstant());
        response.setExpiresAt(claims.getExpiration().toInstant());
        response.setJwtId(claims.getId());
        response.setTokenType(claims.get("token_type", String.class));
    }

    private PrivateKey decryptPrivateKey(String encryptedPrivateKey) throws Exception {
        // Implement decryption logic here
        // This is a placeholder - implement actual decryption
        byte[] keyBytes = Base64.getDecoder().decode(encryptedPrivateKey);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(spec);
    }

    private PublicKey parsePublicKey(String publicKeyString) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyString);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }

    private String hashToken(String token) {
        // Implement secure hashing (e.g., SHA-256)
        return Integer.toString(token.hashCode()); // Placeholder
    }
}