package com.enterprise.auth_service.filter;

import com.enterprise.auth_service.dto.TokenValidationResponse;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Custom authentication token for JWT-based authentication
 */
@Getter
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final String userId;
    private final TokenValidationResponse tokenValidationResponse;
    private JwtAuthenticationDetails authenticationDetails;

    public JwtAuthenticationToken(String userId, 
                                TokenValidationResponse tokenValidationResponse,
                                Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.userId = userId;
        this.tokenValidationResponse = tokenValidationResponse;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null; // JWT tokens don't expose credentials
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }

    @Override
    public void setDetails(Object details) {
        if (details instanceof JwtAuthenticationDetails) {
            this.authenticationDetails = (JwtAuthenticationDetails) details;
        }
        super.setDetails(details);
    }

    public String getTokenId() {
        return tokenValidationResponse.getJwtId();
    }

    public String getClientId() {
        return tokenValidationResponse.getClientId();
    }

    public String getTokenType() {
        return tokenValidationResponse.getTokenType();
    }

    @Override
    public String getName() {
        return userId;
    }
}