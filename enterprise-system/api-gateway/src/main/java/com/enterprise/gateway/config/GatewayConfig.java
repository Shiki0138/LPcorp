package com.enterprise.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {
    
    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        // Rate limiting by user (when we have authentication)
        return exchange -> Mono.just(exchange.getRequest()
                .getHeaders()
                .getFirst("X-User-Id") != null ? 
                exchange.getRequest().getHeaders().getFirst("X-User-Id") : 
                "anonymous");
    }
    
    @Bean
    public KeyResolver ipKeyResolver() {
        // Rate limiting by IP address
        return exchange -> Mono.just(exchange.getRequest()
                .getRemoteAddress()
                .getAddress()
                .getHostAddress());
    }
    
    @Bean
    public KeyResolver apiKeyResolver() {
        // Rate limiting by API key
        return exchange -> Mono.just(exchange.getRequest()
                .getHeaders()
                .getFirst("X-API-Key") != null ?
                exchange.getRequest().getHeaders().getFirst("X-API-Key") :
                "no-api-key");
    }
}