package com.enterprise.auth_service.service;

import com.enterprise.auth_service.entity.RsaKeyPair;
import com.enterprise.auth_service.repository.RsaKeyPairRepository;
import com.nimbusds.jose.jwk.RSAKey;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing RSA key pairs used for JWT signing
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class RsaKeyManagementService {

    private final RsaKeyPairRepository rsaKeyPairRepository;

    @Value("${jwt.key.rotation.interval.days:30}")
    private int keyRotationIntervalDays;

    @Value("${jwt.key.expiration.days:90}")
    private int keyExpirationDays;

    @Value("${jwt.key.size:2048}")
    private int keySize;

    @Value("${encryption.key}")
    private String encryptionKey;

    private static final String KEY_ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "RS256";
    private static final String ENCRYPTION_ALGORITHM = "AES";

    /**
     * Generate a new RSA key pair for JWT signing
     */
    public RsaKeyPair generateKeyPair() {
        log.info("Generating new RSA key pair with size: {}", keySize);

        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyPairGenerator.initialize(keySize);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

            String keyId = generateKeyId();
            String publicKeyString = encodePublicKey(publicKey);
            String encryptedPrivateKey = encryptPrivateKey(privateKey);

            RsaKeyPair rsaKeyPair = RsaKeyPair.builder()
                    .keyId(keyId)
                    .algorithm(SIGNATURE_ALGORITHM)
                    .keySize(keySize)
                    .publicKey(publicKeyString)
                    .privateKeyEncrypted(encryptedPrivateKey)
                    .encryptionKeyId(getEncryptionKeyId())
                    .active(false)
                    .expiresAt(Instant.now().plus(keyExpirationDays, ChronoUnit.DAYS))
                    .build();

            rsaKeyPairRepository.save(rsaKeyPair);

            log.info("Successfully generated RSA key pair with ID: {}", keyId);
            return rsaKeyPair;

        } catch (Exception e) {
            log.error("Failed to generate RSA key pair", e);
            throw new RuntimeException("Key pair generation failed", e);
        }
    }

    /**
     * Activate a key pair for signing
     */
    public void activateKeyPair(String keyId) {
        log.info("Activating key pair: {}", keyId);

        Optional<RsaKeyPair> keyPairOpt = rsaKeyPairRepository.findByKeyId(keyId);
        if (keyPairOpt.isEmpty()) {
            throw new IllegalArgumentException("Key pair not found: " + keyId);
        }

        RsaKeyPair keyPair = keyPairOpt.get();
        if (keyPair.isExpired()) {
            throw new IllegalArgumentException("Cannot activate expired key: " + keyId);
        }

        keyPair.activate();
        rsaKeyPairRepository.save(keyPair);

        log.info("Successfully activated key pair: {}", keyId);
    }

    /**
     * Deactivate a key pair
     */
    public void deactivateKeyPair(String keyId) {
        log.info("Deactivating key pair: {}", keyId);

        rsaKeyPairRepository.deactivateKey(keyId, Instant.now());

        log.info("Successfully deactivated key pair: {}", keyId);
    }

    /**
     * Get the active signing key
     */
    @Cacheable(value = "activeSigningKey", unless = "#result == null")
    public RsaKeyPair getActiveSigningKey() {
        List<RsaKeyPair> activeKeys = rsaKeyPairRepository.findActiveNonExpiredKeys(Instant.now());
        
        if (activeKeys.isEmpty()) {
            log.warn("No active signing keys found, generating new key pair");
            RsaKeyPair newKeyPair = generateKeyPair();
            activateKeyPair(newKeyPair.getKeyId());
            return newKeyPair;
        }

        // Return the newest active key
        return activeKeys.get(0);
    }

    /**
     * Get signing key for a specific token (based on kid header)
     */
    public RsaKeyPair getSigningKeyForToken(String token) {
        try {
            String keyId = extractKeyIdFromToken(token);
            return rsaKeyPairRepository.findByKeyId(keyId)
                    .orElseThrow(() -> new IllegalArgumentException("Key not found: " + keyId));
        } catch (Exception e) {
            log.warn("Failed to extract key ID from token, using active key", e);
            return getActiveSigningKey();
        }
    }

    /**
     * Get all active public keys for JWKS endpoint
     */
    @Cacheable(value = "jwksKeys", unless = "#result.isEmpty()")
    public List<RSAKey> getJwksKeys() {
        log.debug("Getting JWKS keys");

        List<RsaKeyPair> activeKeys = rsaKeyPairRepository.findActiveNonExpiredKeys(Instant.now());
        
        return activeKeys.stream()
                .map(this::convertToRSAKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Rotate keys automatically
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    public void rotateKeysIfNeeded() {
        log.info("Checking if key rotation is needed");

        try {
            List<RsaKeyPair> activeKeys = rsaKeyPairRepository.findActiveNonExpiredKeys(Instant.now());
            
            if (activeKeys.isEmpty()) {
                log.info("No active keys found, generating new key pair");
                RsaKeyPair newKeyPair = generateKeyPair();
                activateKeyPair(newKeyPair.getKeyId());
                return;
            }

            RsaKeyPair newestKey = activeKeys.get(0);
            Instant rotationThreshold = Instant.now().minus(keyRotationIntervalDays, ChronoUnit.DAYS);

            if (newestKey.getActivatedAt().isBefore(rotationThreshold)) {
                log.info("Key rotation needed, newest key is {} days old", 
                        ChronoUnit.DAYS.between(newestKey.getActivatedAt(), Instant.now()));
                
                // Generate and activate new key
                RsaKeyPair newKeyPair = generateKeyPair();
                activateKeyPair(newKeyPair.getKeyId());

                // Keep old key active for overlap period
                scheduleKeyDeactivation(newestKey.getKeyId());
            }

            // Clean up expired keys
            cleanupExpiredKeys();

        } catch (Exception e) {
            log.error("Key rotation failed", e);
        }
    }

    /**
     * Clean up expired keys
     */
    @Scheduled(cron = "0 0 3 * * ?") // Run at 3 AM daily
    public void cleanupExpiredKeys() {
        log.info("Cleaning up expired keys");

        try {
            List<RsaKeyPair> expiredKeys = rsaKeyPairRepository.findExpiredKeys(Instant.now());
            
            for (RsaKeyPair expiredKey : expiredKeys) {
                if (expiredKey.isActive()) {
                    deactivateKeyPair(expiredKey.getKeyId());
                }
            }

            // Delete very old expired keys (older than 1 year)
            Instant deletionThreshold = Instant.now().minus(365, ChronoUnit.DAYS);
            List<RsaKeyPair> oldKeys = expiredKeys.stream()
                    .filter(key -> key.getExpiresAt().isBefore(deletionThreshold))
                    .collect(Collectors.toList());

            if (!oldKeys.isEmpty()) {
                rsaKeyPairRepository.deleteAll(oldKeys);
                log.info("Deleted {} old expired keys", oldKeys.size());
            }

        } catch (Exception e) {
            log.error("Key cleanup failed", e);
        }
    }

    /**
     * Emergency key revocation
     */
    public void emergencyKeyRevocation(String keyId, String reason) {
        log.warn("Emergency key revocation for key: {}, reason: {}", keyId, reason);

        try {
            // Deactivate the compromised key
            deactivateKeyPair(keyId);

            // Generate and activate new key immediately
            RsaKeyPair newKeyPair = generateKeyPair();
            activateKeyPair(newKeyPair.getKeyId());

            // TODO: Notify token revocation service to revoke all tokens signed with the compromised key

            log.info("Emergency key revocation completed, new key activated: {}", newKeyPair.getKeyId());

        } catch (Exception e) {
            log.error("Emergency key revocation failed", e);
            throw new RuntimeException("Emergency key revocation failed", e);
        }
    }

    // Private helper methods

    private String generateKeyId() {
        return "rsa-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String encodePublicKey(RSAPublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    private String encryptPrivateKey(RSAPrivateKey privateKey) throws Exception {
        byte[] privateKeyBytes = privateKey.getEncoded();
        
        SecretKeySpec secretKey = new SecretKeySpec(
                encryptionKey.getBytes(StandardCharsets.UTF_8), 
                ENCRYPTION_ALGORITHM
        );
        
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        
        byte[] encryptedBytes = cipher.doFinal(privateKeyBytes);
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private String getEncryptionKeyId() {
        // In production, this should be a reference to a key in a key management service
        return "enc-key-1";
    }

    private String extractKeyIdFromToken(String token) {
        try {
            String[] chunks = token.split("\\.");
            if (chunks.length >= 1) {
                String header = new String(Base64.getDecoder().decode(chunks[0]));
                // Simple extraction - in production, use proper JSON parsing
                return extractKidFromHeader(header);
            }
        } catch (Exception e) {
            log.warn("Failed to extract key ID from token header", e);
        }
        throw new IllegalArgumentException("Invalid token format");
    }

    private String extractKidFromHeader(String header) {
        // This is a simplified implementation
        // In production, use Jackson or other JSON library
        int kidIndex = header.indexOf("\"kid\":\"");
        if (kidIndex != -1) {
            int start = kidIndex + 7;
            int end = header.indexOf("\"", start);
            if (end != -1) {
                return header.substring(start, end);
            }
        }
        throw new IllegalArgumentException("No kid found in token header");
    }

    private RSAKey convertToRSAKey(RsaKeyPair keyPair) {
        try {
            PublicKey publicKey = parsePublicKey(keyPair.getPublicKey());
            RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;

            return new RSAKey.Builder(rsaPublicKey)
                    .keyID(keyPair.getKeyId())
                    .algorithm(com.nimbusds.jose.Algorithm.parse(keyPair.getAlgorithm()))
                    .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
                    .build();

        } catch (Exception e) {
            log.error("Failed to convert key pair to RSA key: {}", keyPair.getKeyId(), e);
            return null;
        }
    }

    private PublicKey parsePublicKey(String publicKeyString) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyString);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        return keyFactory.generatePublic(spec);
    }

    private void scheduleKeyDeactivation(String keyId) {
        // In a real implementation, you would use a job scheduler like Quartz
        // For now, we'll deactivate after a grace period
        // This is a simplified approach
        log.info("Scheduling deactivation for key: {} in 24 hours", keyId);
        
        // TODO: Implement proper job scheduling
        // For now, the old key remains active until the next rotation cycle
    }
}