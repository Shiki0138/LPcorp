package com.enterprise.auth_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.Instant;
import java.util.Set;

/**
 * Entity representing JWT tokens stored in the database
 */
@Entity
@Table(name = "jwt_tokens", indexes = {
    @Index(name = "idx_jwt_token_id", columnList = "tokenId"),
    @Index(name = "idx_jwt_user_id", columnList = "userId"),
    @Index(name = "idx_jwt_client_id", columnList = "clientId"),
    @Index(name = "idx_jwt_expires_at", columnList = "expiresAt"),
    @Index(name = "idx_jwt_revoked", columnList = "revoked")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_id", unique = true, nullable = false)
    private String tokenId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false)
    private TokenType tokenType;

    @Column(name = "token_hash", nullable = false, length = 512)
    private String tokenHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "jwt_token_scopes", joinColumns = @JoinColumn(name = "token_id"))
    @Column(name = "scope")
    private Set<String> scopes;

    @Column(name = "audience", nullable = false)
    private String audience;

    @Column(name = "issuer", nullable = false)
    private String issuer;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "not_before")
    private Instant notBefore;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_reason")
    private String revokedReason;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (issuedAt == null) {
            issuedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum TokenType {
        ACCESS_TOKEN,
        REFRESH_TOKEN,
        ID_TOKEN
    }
}