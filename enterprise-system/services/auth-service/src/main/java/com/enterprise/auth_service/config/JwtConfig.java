package com.enterprise.auth_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT configuration properties
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtConfig {

    /**
     * JWT issuer claim
     */
    private String issuer = "https://auth.enterprise.com";

    /**
     * Default audience for JWT tokens
     */
    private String audience = "enterprise-api";

    /**
     * Access token configuration
     */
    private AccessToken accessToken = new AccessToken();

    /**
     * Refresh token configuration
     */
    private RefreshToken refreshToken = new RefreshToken();

    /**
     * ID token configuration
     */
    private IdToken idToken = new IdToken();

    /**
     * Key management configuration
     */
    private Key key = new Key();

    @Data
    public static class AccessToken {
        /**
         * Access token expiration in seconds (default: 15 minutes)
         */
        private long expiration = 900;

        /**
         * Maximum number of active access tokens per user
         */
        private int maxActiveTokens = 10;
    }

    @Data
    public static class RefreshToken {
        /**
         * Refresh token expiration in seconds (default: 7 days)
         */
        private long expiration = 604800;

        /**
         * Maximum number of active refresh tokens per user
         */
        private int maxActiveTokens = 5;

        /**
         * Whether to rotate refresh tokens on use
         */
        private boolean rotateOnUse = true;
    }

    @Data
    public static class IdToken {
        /**
         * ID token expiration in seconds (default: 1 hour)
         */
        private long expiration = 3600;

        /**
         * Whether to include profile information in ID tokens
         */
        private boolean includeProfile = true;
    }

    @Data
    public static class Key {
        /**
         * RSA key size for JWT signing
         */
        private int size = 2048;

        /**
         * Key rotation interval in days
         */
        private Rotation rotation = new Rotation();

        /**
         * Key expiration in days
         */
        private int expirationDays = 90;

        @Data
        public static class Rotation {
            /**
             * Automatic key rotation interval in days
             */
            private int intervalDays = 30;

            /**
             * Whether to enable automatic key rotation
             */
            private boolean enabled = true;

            /**
             * Grace period for old keys in days
             */
            private int gracePeriodDays = 7;
        }
    }
}