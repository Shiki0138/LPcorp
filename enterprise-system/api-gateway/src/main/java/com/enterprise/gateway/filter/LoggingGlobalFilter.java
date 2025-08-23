package com.enterprise.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
@Slf4j
public class LoggingGlobalFilter implements GlobalFilter, Ordered {
    
    private static final String START_TIME = "startTime";
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getAttributes().put(START_TIME, Instant.now().toEpochMilli());
        
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().toString();
        String requestId = exchange.getRequest().getHeaders().getFirst("X-Request-Id");
        
        log.info("Incoming request - Method: {}, Path: {}, Request-Id: {}", 
                method, path, requestId);
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            Long startTime = exchange.getAttribute(START_TIME);
            if (startTime != null) {
                long executeTime = Instant.now().toEpochMilli() - startTime;
                int statusCode = exchange.getResponse().getStatusCode() != null ? 
                        exchange.getResponse().getStatusCode().value() : 0;
                        
                log.info("Outgoing response - Method: {}, Path: {}, Status: {}, Duration: {}ms, Request-Id: {}",
                        method, path, statusCode, executeTime, requestId);
                        
                exchange.getResponse().getHeaders().add("X-Response-Time", executeTime + "ms");
            }
        }));
    }
    
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}