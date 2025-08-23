package com.enterprise.auth_service.config;

import com.enterprise.auth_service.filter.RateLimitFilter;
import com.enterprise.auth_service.filter.SecurityAuditFilter;
import com.enterprise.auth_service.service.RateLimitService;
import com.enterprise.auth_service.service.SecurityEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Configuration for security filters
 */
@Configuration
@RequiredArgsConstructor
public class SecurityFilterConfig {

    private final RateLimitService rateLimitService;
    private final SecurityEventService securityEventService;

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter() {
        FilterRegistrationBean<RateLimitFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RateLimitFilter(rateLimitService));
        registrationBean.addUrlPatterns("/oauth2/*", "/api/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<SecurityAuditFilter> securityAuditFilter() {
        FilterRegistrationBean<SecurityAuditFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new SecurityAuditFilter(securityEventService));
        registrationBean.addUrlPatterns("/oauth2/*", "/api/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registrationBean;
    }
}