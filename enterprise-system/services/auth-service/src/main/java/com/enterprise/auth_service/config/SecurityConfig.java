package com.enterprise.auth_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.security.interfaces.RSAPublicKey;

/**
 * Security configuration for the auth service
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${jwt.public-key:}")
    private String publicKeyString;

    @Value("${security.admin.username:admin}")
    private String adminUsername;

    @Value("${security.admin.password:admin123}")
    private String adminPassword;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints
                        .antMatchers("/actuator/health", "/actuator/info").permitAll()
                        .antMatchers("/.well-known/**").permitAll()
                        .antMatchers("/api/v1/tokens/health").permitAll()
                        .antMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        
                        // Token operations
                        .antMatchers("/api/v1/tokens/refresh").permitAll()
                        .antMatchers("/api/v1/tokens/introspect").permitAll()
                        
                        // Protected endpoints
                        .antMatchers("/api/v1/tokens/**").authenticated()
                        .antMatchers("/api/v1/keys/**").hasRole("ADMIN")
                        
                        // Default - require authentication
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                        )
                )
                .httpBasic(); // For admin endpoints

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        if (publicKeyString != null && !publicKeyString.isEmpty()) {
            try {
                // Parse the public key for JWT validation
                // This is simplified - in production, you'd integrate with your key management
                return NimbusJwtDecoder.withJwkSetUri("/.well-known/jwks.json").build();
            } catch (Exception e) {
                // Fallback to a dummy decoder for development
                return token -> {
                    throw new RuntimeException("JWT decoder not properly configured");
                };
            }
        }
        
        // Development mode - create a dummy decoder
        return token -> {
            throw new RuntimeException("JWT decoder not configured for development");
        };
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
                .username(adminUsername)
                .password(passwordEncoder().encode(adminPassword))
                .roles("ADMIN", "USER")
                .authorities("SCOPE_token:generate", "SCOPE_token:validate", "SCOPE_token:revoke", "SCOPE_token:introspect", "SCOPE_token:read")
                .build();

        UserDetails service = User.builder()
                .username("service-client")
                .password(passwordEncoder().encode("service-secret"))
                .roles("SERVICE")
                .authorities("SCOPE_token:validate", "SCOPE_token:introspect")
                .build();

        return new InMemoryUserDetailsManager(admin, service);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}