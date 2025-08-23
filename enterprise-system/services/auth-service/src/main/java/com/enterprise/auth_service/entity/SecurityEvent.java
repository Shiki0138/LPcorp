package com.enterprise.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Security Event entity for audit logging
 */
@Entity
@Table(name = "security_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "client_id", length = 100)
    private String clientId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "success")
    private Boolean success;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "resource", length = 500)
    private String resource;

    @Column(name = "action", length = 100)
    private String action;

    @Lob
    @Column(name = "additional_data")
    private String additionalData;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "severity", length = 20)
    private String severity;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "geo_location", length = 200)
    private String geoLocation;

    @Column(name = "device_fingerprint", length = 200)
    private String deviceFingerprint;
}