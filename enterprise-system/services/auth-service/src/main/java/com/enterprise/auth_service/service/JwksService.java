package com.enterprise.auth_service.service;

import com.enterprise.auth_service.dto.JwksResponse;
import com.enterprise.auth_service.entity.RsaKeyPair;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for JSON Web Key Set (JWKS) operations
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JwksService {

    private final RsaKeyManagementService keyManagementService;

    /**
     * Get JWKS response with all active public keys
     */
    @Cacheable(value = "jwks", unless = "#result == null")
    public JwksResponse getJwks() {
        log.debug("Generating JWKS response");

        try {
            List<RSAKey> rsaKeys = keyManagementService.getJwksKeys();
            
            List<JwksResponse.JwkDto> jwkDtos = rsaKeys.stream()
                    .map(this::convertToJwkDto)
                    .collect(Collectors.toList());

            JwksResponse response = JwksResponse.builder()
                    .keys(jwkDtos)
                    .build();

            log.debug("Generated JWKS response with {} keys", jwkDtos.size());
            return response;

        } catch (Exception e) {
            log.error("Failed to generate JWKS response", e);
            throw new RuntimeException("JWKS generation failed", e);
        }
    }

    /**
     * Get JWKS for a specific key ID
     */
    public JwksResponse.JwkDto getJwkForKeyId(String keyId) {
        log.debug("Getting JWK for key ID: {}", keyId);

        List<RSAKey> rsaKeys = keyManagementService.getJwksKeys();
        
        return rsaKeys.stream()
                .filter(key -> keyId.equals(key.getKeyID()))
                .findFirst()
                .map(this::convertToJwkDto)
                .orElse(null);
    }

    /**
     * Validate JWKS format and content
     */
    public boolean validateJwks(JwksResponse jwks) {
        if (jwks == null || jwks.getKeys() == null || jwks.getKeys().isEmpty()) {
            return false;
        }

        return jwks.getKeys().stream()
                .allMatch(this::validateJwk);
    }

    private JwksResponse.JwkDto convertToJwkDto(RSAKey rsaKey) {
        try {
            RSAPublicKey publicKey = rsaKey.toRSAPublicKey();
            
            return JwksResponse.JwkDto.builder()
                    .kty("RSA")
                    .use("sig")
                    .kid(rsaKey.getKeyID())
                    .alg(rsaKey.getAlgorithm() != null ? rsaKey.getAlgorithm().getName() : "RS256")
                    .n(encodeRSAParameter(publicKey.getModulus().toByteArray()))
                    .e(encodeRSAParameter(publicKey.getPublicExponent().toByteArray()))
                    .build();

        } catch (Exception e) {
            log.error("Failed to convert RSA key to JWK DTO: {}", rsaKey.getKeyID(), e);
            return null;
        }
    }

    private String encodeRSAParameter(byte[] parameter) {
        // Remove leading zero byte if present (for positive big integers)
        if (parameter.length > 1 && parameter[0] == 0) {
            byte[] trimmed = new byte[parameter.length - 1];
            System.arraycopy(parameter, 1, trimmed, 0, trimmed.length);
            parameter = trimmed;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(parameter);
    }

    private boolean validateJwk(JwksResponse.JwkDto jwk) {
        return jwk != null &&
               "RSA".equals(jwk.getKty()) &&
               "sig".equals(jwk.getUse()) &&
               jwk.getKid() != null &&
               jwk.getN() != null &&
               jwk.getE() != null;
    }
}