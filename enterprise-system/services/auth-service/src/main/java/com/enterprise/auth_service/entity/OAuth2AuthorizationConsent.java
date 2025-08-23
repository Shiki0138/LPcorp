package com.enterprise.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * OAuth2 Authorization Consent entity for storing user consent decisions
 */
@Entity
@Table(name = "oauth2_authorization_consents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(OAuth2AuthorizationConsentId.class)
public class OAuth2AuthorizationConsent {

    @Id
    @Column(name = "registered_client_id", length = 100)
    private String registeredClientId;

    @Id
    @Column(name = "principal_name", length = 200)
    private String principalName;

    @Column(name = "authorities", nullable = false, length = 1000)
    private String authorities;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;
}