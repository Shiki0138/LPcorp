package com.enterprise.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for JWKS (JSON Web Key Set) endpoint
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwksResponse {

    private List<JwkDto> keys;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JwkDto {
        private String kty;  // Key Type
        private String use;  // Public Key Use
        private String kid;  // Key ID
        private String alg;  // Algorithm
        private String n;    // Modulus
        private String e;    // Exponent
        private List<String> x5c; // X.509 Certificate Chain
        private String x5t;  // X.509 Certificate SHA-1 Thumbprint
        private String x5t256; // X.509 Certificate SHA-256 Thumbprint

        // Additional fields for different key types
        private Map<String, Object> additionalFields;
    }
}