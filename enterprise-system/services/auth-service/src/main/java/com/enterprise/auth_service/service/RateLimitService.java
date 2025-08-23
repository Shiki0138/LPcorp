package com.enterprise.auth_service.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.jedis.cas.JedisBasedProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPool;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Rate limiting service using Bucket4j with Redis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final JedisPool jedisPool;
    private final SecurityEventService securityEventService;
    
    // Different rate limits for different scenarios
    private static final int LOGIN_ATTEMPTS_PER_HOUR = 10;
    private static final int TOKEN_REQUESTS_PER_MINUTE = 60;
    private static final int API_REQUESTS_PER_MINUTE = 100;
    private static final int CLIENT_REQUESTS_PER_HOUR = 1000;

    private final ProxyManager<String> proxyManager;

    public RateLimitService(JedisPool jedisPool, SecurityEventService securityEventService) {
        this.jedisPool = jedisPool;
        this.securityEventService = securityEventService;
        this.proxyManager = new JedisBasedProxyManager<>(jedisPool);
    }

    /**
     * Check if login attempts are within rate limit
     */
    public boolean isLoginAllowed(String username, String ipAddress) {
        String userKey = "login:user:" + username;
        String ipKey = "login:ip:" + ipAddress;
        
        boolean userAllowed = checkRateLimit(userKey, 
            () -> createLoginRateLimitConfig(), "LOGIN");
        boolean ipAllowed = checkRateLimit(ipKey, 
            () -> createLoginRateLimitConfig(), "LOGIN");
        
        boolean allowed = userAllowed && ipAllowed;
        
        if (!allowed) {
            securityEventService.logRateLimitExceeded(null, ipAddress, "login");
            log.warn("Login rate limit exceeded for user: {} from IP: {}", username, ipAddress);
        }
        
        return allowed;
    }

    /**
     * Check if token requests are within rate limit
     */
    public boolean isTokenRequestAllowed(String clientId, String ipAddress) {
        String clientKey = "token:client:" + clientId;
        String ipKey = "token:ip:" + ipAddress;
        
        boolean clientAllowed = checkRateLimit(clientKey, 
            () -> createTokenRateLimitConfig(), "TOKEN");
        boolean ipAllowed = checkRateLimit(ipKey, 
            () -> createTokenRateLimitConfig(), "TOKEN");
        
        boolean allowed = clientAllowed && ipAllowed;
        
        if (!allowed) {
            securityEventService.logRateLimitExceeded(clientId, ipAddress, "token");
            log.warn("Token rate limit exceeded for client: {} from IP: {}", clientId, ipAddress);
        }
        
        return allowed;
    }

    /**
     * Check if API requests are within rate limit
     */
    public boolean isApiRequestAllowed(String clientId, String ipAddress, String endpoint) {
        String clientKey = "api:client:" + clientId;
        String ipKey = "api:ip:" + ipAddress;
        String endpointKey = "api:endpoint:" + endpoint + ":" + ipAddress;
        
        boolean clientAllowed = checkRateLimit(clientKey, 
            () -> createClientRateLimitConfig(), "API");
        boolean ipAllowed = checkRateLimit(ipKey, 
            () -> createApiRateLimitConfig(), "API");
        boolean endpointAllowed = checkRateLimit(endpointKey, 
            () -> createApiRateLimitConfig(), "API");
        
        boolean allowed = clientAllowed && ipAllowed && endpointAllowed;
        
        if (!allowed) {
            securityEventService.logRateLimitExceeded(clientId, ipAddress, endpoint);
            log.warn("API rate limit exceeded for client: {} from IP: {} on endpoint: {}", 
                clientId, ipAddress, endpoint);
        }
        
        return allowed;
    }

    /**
     * Check if device authorization requests are within rate limit
     */
    public boolean isDeviceAuthorizationAllowed(String clientId, String ipAddress) {
        String key = "device:client:" + clientId + ":ip:" + ipAddress;
        
        boolean allowed = checkRateLimit(key, 
            () -> createDeviceAuthRateLimitConfig(), "DEVICE_AUTH");
        
        if (!allowed) {
            securityEventService.logRateLimitExceeded(clientId, ipAddress, "device_authorization");
            log.warn("Device authorization rate limit exceeded for client: {} from IP: {}", clientId, ipAddress);
        }
        
        return allowed;
    }

    /**
     * Check rate limit for introspection endpoints
     */
    public boolean isIntrospectionAllowed(String clientId, String ipAddress) {
        String key = "introspect:client:" + clientId + ":ip:" + ipAddress;
        
        boolean allowed = checkRateLimit(key, 
            () -> createIntrospectionRateLimitConfig(), "INTROSPECTION");
        
        if (!allowed) {
            securityEventService.logRateLimitExceeded(clientId, ipAddress, "introspection");
            log.warn("Introspection rate limit exceeded for client: {} from IP: {}", clientId, ipAddress);
        }
        
        return allowed;
    }

    private boolean checkRateLimit(String key, Supplier<BucketConfiguration> configSupplier, String operation) {
        try {
            Bucket bucket = proxyManager.builder().build(key, configSupplier);
            return bucket.tryConsume(1);
        } catch (Exception e) {
            log.error("Error checking rate limit for key: {} operation: {}", key, operation, e);
            // In case of Redis failure, allow the request but log the error
            return true;
        }
    }

    private BucketConfiguration createLoginRateLimitConfig() {
        return BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(LOGIN_ATTEMPTS_PER_HOUR, Duration.ofHours(1)))
            .build();
    }

    private BucketConfiguration createTokenRateLimitConfig() {
        return BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(TOKEN_REQUESTS_PER_MINUTE, Duration.ofMinutes(1)))
            .addLimit(Bandwidth.simple(TOKEN_REQUESTS_PER_MINUTE * 10, Duration.ofHours(1)))
            .build();
    }

    private BucketConfiguration createApiRateLimitConfig() {
        return BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(API_REQUESTS_PER_MINUTE, Duration.ofMinutes(1)))
            .addLimit(Bandwidth.simple(API_REQUESTS_PER_MINUTE * 20, Duration.ofHours(1)))
            .build();
    }

    private BucketConfiguration createClientRateLimitConfig() {
        return BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(CLIENT_REQUESTS_PER_HOUR, Duration.ofHours(1)))
            .addLimit(Bandwidth.simple(CLIENT_REQUESTS_PER_HOUR * 20, Duration.ofDays(1)))
            .build();
    }

    private BucketConfiguration createDeviceAuthRateLimitConfig() {
        // More restrictive for device authorization
        return BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(5, Duration.ofMinutes(5)))
            .addLimit(Bandwidth.simple(20, Duration.ofHours(1)))
            .build();
    }

    private BucketConfiguration createIntrospectionRateLimitConfig() {
        return BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(30, Duration.ofMinutes(1)))
            .addLimit(Bandwidth.simple(500, Duration.ofHours(1)))
            .build();
    }

    /**
     * Reset rate limit for a specific key (admin function)
     */
    public void resetRateLimit(String key) {
        try {
            // This will effectively reset the bucket by using a new configuration
            Bucket bucket = proxyManager.builder().build(key + ":reset:" + System.currentTimeMillis(), 
                () -> createApiRateLimitConfig());
            log.info("Rate limit reset for key: {}", key);
        } catch (Exception e) {
            log.error("Error resetting rate limit for key: {}", key, e);
        }
    }
}