package com.enterprise.auth_service.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key for OAuth2AuthorizationConsent
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2AuthorizationConsentId implements Serializable {
    
    private String registeredClientId;
    private String principalName;
}