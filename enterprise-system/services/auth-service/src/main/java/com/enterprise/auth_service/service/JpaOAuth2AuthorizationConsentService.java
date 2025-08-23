package com.enterprise.auth_service.service;

import com.enterprise.auth_service.entity.OAuth2AuthorizationConsent;
import com.enterprise.auth_service.repository.OAuth2AuthorizationConsentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * JPA-based implementation of OAuth2AuthorizationConsentService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JpaOAuth2AuthorizationConsentService implements OAuth2AuthorizationConsentService {

    private final OAuth2AuthorizationConsentRepository consentRepository;

    @Override
    public void save(org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent authorizationConsent) {
        log.debug("Saving OAuth2 authorization consent for client: {} and principal: {}", 
            authorizationConsent.getRegisteredClientId(), authorizationConsent.getPrincipalName());
        
        OAuth2AuthorizationConsent entity = OAuth2AuthorizationConsent.builder()
            .registeredClientId(authorizationConsent.getRegisteredClientId())
            .principalName(authorizationConsent.getPrincipalName())
            .authorities(StringUtils.collectionToCommaDelimitedString(
                authorizationConsent.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList()))
            .build();

        consentRepository.save(entity);
    }

    @Override
    public void remove(org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent authorizationConsent) {
        log.debug("Removing OAuth2 authorization consent for client: {} and principal: {}", 
            authorizationConsent.getRegisteredClientId(), authorizationConsent.getPrincipalName());
        
        consentRepository.deleteByRegisteredClientIdAndPrincipalName(
            authorizationConsent.getRegisteredClientId(),
            authorizationConsent.getPrincipalName());
    }

    @Override
    public org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent findById(
            String registeredClientId, String principalName) {
        log.debug("Finding OAuth2 authorization consent for client: {} and principal: {}", 
            registeredClientId, principalName);
        
        return consentRepository.findByRegisteredClientIdAndPrincipalName(registeredClientId, principalName)
            .map(this::toObject)
            .orElse(null);
    }

    private org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent toObject(
            OAuth2AuthorizationConsent entity) {
        String registeredClientId = entity.getRegisteredClientId();
        org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent.Builder builder = 
            org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent
                .withId(registeredClientId, entity.getPrincipalName());

        Set<String> authorities = StringUtils.commaDelimitedListToSet(entity.getAuthorities());
        for (String authority : authorities) {
            builder.authority(new SimpleGrantedAuthority(authority));
        }

        return builder.build();
    }

    // Simple implementation of GrantedAuthority for authorities
    private static class SimpleGrantedAuthority implements GrantedAuthority {
        private final String authority;

        public SimpleGrantedAuthority(String authority) {
            this.authority = authority;
        }

        @Override
        public String getAuthority() {
            return authority;
        }
    }
}