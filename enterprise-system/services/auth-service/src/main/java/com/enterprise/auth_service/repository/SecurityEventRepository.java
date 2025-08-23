package com.enterprise.auth_service.repository;

import com.enterprise.auth_service.entity.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for SecurityEvent entities
 */
@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {

    List<SecurityEvent> findByUsernameAndEventTypeOrderByCreatedAtDesc(String username, String eventType);

    List<SecurityEvent> findByIpAddressAndCreatedAtAfterOrderByCreatedAtDesc(String ipAddress, Instant since);

    @Query("SELECT COUNT(e) FROM SecurityEvent e WHERE e.username = :username AND e.eventType = :eventType AND e.success = false AND e.createdAt > :since")
    long countFailedAttempts(@Param("username") String username, @Param("eventType") String eventType, @Param("since") Instant since);

    @Query("SELECT COUNT(e) FROM SecurityEvent e WHERE e.ipAddress = :ipAddress AND e.eventType = :eventType AND e.success = false AND e.createdAt > :since")
    long countFailedAttemptsByIp(@Param("ipAddress") String ipAddress, @Param("eventType") String eventType, @Param("since") Instant since);

    @Query("SELECT e FROM SecurityEvent e WHERE e.severity IN ('HIGH', 'CRITICAL') AND e.createdAt > :since ORDER BY e.createdAt DESC")
    List<SecurityEvent> findHighSeverityEvents(@Param("since") Instant since);

    @Query("SELECT e FROM SecurityEvent e WHERE e.riskScore >= :threshold AND e.createdAt > :since ORDER BY e.riskScore DESC, e.createdAt DESC")
    List<SecurityEvent> findHighRiskEvents(@Param("threshold") Integer threshold, @Param("since") Instant since);
}