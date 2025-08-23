package com.enterprise.auth_service.service;

import com.enterprise.auth_service.dto.TokenRequest;
import com.enterprise.auth_service.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Service for managing service-to-service authentication tokens
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServiceTokenService {

    private final JwtTokenService jwtTokenService;

    private static final String SERVICE_USER_PREFIX = "service:";
    private static final long SERVICE_TOKEN_EXPIRATION = 3600; // 1 hour

    /**
     * Generate a service token for inter-service communication
     */
    @Cacheable(value = "serviceTokens", key = "#serviceId + ':' + #targetService", unless = "#result == null")
    public TokenResponse generateServiceToken(String serviceId, String targetService, Set<String> scopes) {
        log.debug("Generating service token for service: {} targeting: {}", serviceId, targetService);

        try {
            TokenRequest request = TokenRequest.builder()
                    .userId(SERVICE_USER_PREFIX + serviceId)
                    .clientId(serviceId)
                    .scopes(scopes)
                    .audience(targetService)
                    .subject(serviceId)
                    .serviceToken(true)
                    .accessTokenExpirationSeconds(SERVICE_TOKEN_EXPIRATION)
                    .build();

            // Add service-specific claims
            request.getAdditionalClaims().put("service_id", serviceId);
            request.getAdditionalClaims().put("target_service", targetService);
            request.getAdditionalClaims().put("token_purpose", "service_to_service");

            TokenResponse response = jwtTokenService.generateTokens(request);
            
            log.info("Successfully generated service token for service: {}", serviceId);
            return response;

        } catch (Exception e) {
            log.error("Failed to generate service token for service: {}", serviceId, e);
            throw new RuntimeException("Service token generation failed", e);
        }
    }

    /**
     * Generate a token for a specific microservice with predefined scopes
     */
    public TokenResponse generateMicroserviceToken(MicroserviceType microservice) {
        String serviceId = microservice.getServiceId();
        Set<String> scopes = microservice.getDefaultScopes();
        String audience = "enterprise-microservices";

        return generateServiceToken(serviceId, audience, scopes);
    }

    /**
     * Generate a gateway token for API Gateway
     */
    public TokenResponse generateGatewayToken() {
        return generateMicroserviceToken(MicroserviceType.API_GATEWAY);
    }

    /**
     * Validate service token and extract service information
     */
    public boolean validateServiceToken(String token, String expectedServiceId) {
        try {
            // Use the main JWT token service for validation
            var validationRequest = com.enterprise.auth_service.dto.TokenValidationRequest.builder()
                    .token(token)
                    .includeTokenInfo(true)
                    .build();

            var validationResponse = jwtTokenService.validateToken(validationRequest);

            if (!validationResponse.isValid()) {
                return false;
            }

            // Check if it's a service token
            String userId = validationResponse.getUserId();
            if (!userId.startsWith(SERVICE_USER_PREFIX)) {
                return false;
            }

            // Extract service ID and validate
            String serviceId = userId.substring(SERVICE_USER_PREFIX.length());
            return expectedServiceId.equals(serviceId);

        } catch (Exception e) {
            log.error("Service token validation failed", e);
            return false;
        }
    }

    /**
     * Enum defining microservice types and their default scopes
     */
    public enum MicroserviceType {
        API_GATEWAY("api-gateway", Set.of("gateway:route", "gateway:auth", "token:validate")),
        USER_SERVICE("user-service", Set.of("user:read", "user:write", "profile:read")),
        ORDER_SERVICE("order-service", Set.of("order:read", "order:write", "payment:read")),
        PAYMENT_SERVICE("payment-service", Set.of("payment:read", "payment:write", "order:read")),
        NOTIFICATION_SERVICE("notification-service", Set.of("notification:send", "user:read")),
        ANALYTICS_SERVICE("analytics-service", Set.of("analytics:read", "analytics:write", "data:read")),
        INVENTORY_SERVICE("inventory-service", Set.of("inventory:read", "inventory:write", "product:read")),
        REPORTING_SERVICE("reporting-service", Set.of("report:generate", "data:read", "analytics:read"));

        private final String serviceId;
        private final Set<String> defaultScopes;

        MicroserviceType(String serviceId, Set<String> defaultScopes) {
            this.serviceId = serviceId;
            this.defaultScopes = defaultScopes;
        }

        public String getServiceId() {
            return serviceId;
        }

        public Set<String> getDefaultScopes() {
            return defaultScopes;
        }

        public static MicroserviceType fromServiceId(String serviceId) {
            for (MicroserviceType type : values()) {
                if (type.serviceId.equals(serviceId)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown service ID: " + serviceId);
        }
    }
}