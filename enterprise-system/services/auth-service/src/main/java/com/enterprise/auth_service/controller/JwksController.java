package com.enterprise.auth_service.controller;

import com.enterprise.auth_service.dto.JwksResponse;
import com.enterprise.auth_service.service.JwksService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * Controller for JSON Web Key Set (JWKS) endpoints
 */
@RestController
@RequestMapping("/.well-known")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", methods = {RequestMethod.GET})
public class JwksController {

    private final JwksService jwksService;

    /**
     * Get JSON Web Key Set
     * This endpoint is used by clients to retrieve public keys for JWT verification
     */
    @GetMapping("/jwks.json")
    public ResponseEntity<JwksResponse> getJwks() {
        log.debug("JWKS endpoint accessed");

        try {
            JwksResponse jwks = jwksService.getJwks();
            
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS)
                            .mustRevalidate()
                            .cachePublic())
                    .header("Content-Type", "application/json")
                    .body(jwks);

        } catch (Exception e) {
            log.error("Failed to serve JWKS", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get specific JWK by key ID
     */
    @GetMapping("/jwks/{keyId}")
    public ResponseEntity<JwksResponse.JwkDto> getJwk(@PathVariable String keyId) {
        log.debug("JWK endpoint accessed for key ID: {}", keyId);

        try {
            JwksResponse.JwkDto jwk = jwksService.getJwkForKeyId(keyId);
            
            if (jwk == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS)
                            .mustRevalidate()
                            .cachePublic())
                    .header("Content-Type", "application/json")
                    .body(jwk);

        } catch (Exception e) {
            log.error("Failed to serve JWK for key ID: {}", keyId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Health check for JWKS endpoint
     */
    @GetMapping("/jwks/health")
    public ResponseEntity<String> health() {
        try {
            JwksResponse jwks = jwksService.getJwks();
            
            if (jwks == null || jwks.getKeys() == null || jwks.getKeys().isEmpty()) {
                return ResponseEntity.status(503).body("No active keys available");
            }

            return ResponseEntity.ok("JWKS endpoint healthy with " + jwks.getKeys().size() + " active keys");

        } catch (Exception e) {
            log.error("JWKS health check failed", e);
            return ResponseEntity.status(503).body("JWKS endpoint unhealthy");
        }
    }
}