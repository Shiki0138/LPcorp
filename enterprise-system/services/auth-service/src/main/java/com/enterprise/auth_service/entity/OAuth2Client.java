package com.enterprise.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Set;

/**
 * OAuth2 Client entity for client registration and management
 */
@Entity
@Table(name = "oauth2_clients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"clientSecret"})
public class OAuth2Client {

    @Id
    @Column(name = "client_id", length = 100)
    private String clientId;

    @Column(name = "client_secret", nullable = false, length = 200)
    private String clientSecret;

    @Column(name = "client_name", nullable = false, length = 200)
    private String clientName;

    @ElementCollection(targetClass = String.class)
    @CollectionTable(name = "oauth2_client_redirect_uris", 
                    joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "redirect_uri", length = 500)
    private Set<String> redirectUris;

    @ElementCollection(targetClass = String.class)
    @CollectionTable(name = "oauth2_client_grant_types", 
                    joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "grant_type", length = 50)
    private Set<String> authorizedGrantTypes;

    @ElementCollection(targetClass = String.class)
    @CollectionTable(name = "oauth2_client_scopes", 
                    joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "scope", length = 100)
    private Set<String> scopes;

    @Column(name = "access_token_validity_seconds")
    private Integer accessTokenValiditySeconds;

    @Column(name = "refresh_token_validity_seconds")
    private Integer refreshTokenValiditySeconds;

    @Column(name = "require_authorization_consent")
    @Builder.Default
    private Boolean requireAuthorizationConsent = false;

    @Column(name = "require_proof_key")
    @Builder.Default
    private Boolean requireProofKey = false;

    @Column(name = "client_authentication_methods", length = 500)
    private String clientAuthenticationMethods;

    @Column(name = "authorization_code_time_to_live")
    private Integer authorizationCodeTimeToLive;

    @Column(name = "device_code_time_to_live")
    private Integer deviceCodeTimeToLive;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "rate_limit_per_hour")
    @Builder.Default
    private Integer rateLimitPerHour = 1000;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;
}