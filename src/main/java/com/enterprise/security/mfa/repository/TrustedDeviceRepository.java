package com.enterprise.security.mfa.repository;

import com.enterprise.security.mfa.model.TrustedDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, UUID> {
    
    Optional<TrustedDevice> findByUserIdAndDeviceFingerprintAndIsActiveTrue(String userId, String deviceFingerprint);
    
    List<TrustedDevice> findByUserIdAndIsActiveTrue(String userId);
    
    @Query("SELECT td FROM TrustedDevice td WHERE td.userId = :userId AND td.deviceFingerprint = :fingerprint AND td.isActive = true AND td.expiresAt > :now")
    Optional<TrustedDevice> findValidTrustedDevice(@Param("userId") String userId, 
                                                  @Param("fingerprint") String fingerprint, 
                                                  @Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(td) FROM TrustedDevice td WHERE td.userId = :userId AND td.isActive = true AND td.expiresAt > :now")
    long countActiveTrustedDevices(@Param("userId") String userId, @Param("now") LocalDateTime now);
    
    @Modifying
    @Transactional
    @Query("UPDATE TrustedDevice td SET td.isActive = false, td.revokedAt = :revokedAt, td.revokedBy = :revokedBy, td.revokeReason = :reason WHERE td.userId = :userId AND td.isActive = true")
    void revokeAllUserDevices(@Param("userId") String userId, 
                             @Param("revokedAt") LocalDateTime revokedAt, 
                             @Param("revokedBy") String revokedBy, 
                             @Param("reason") String reason);
    
    @Modifying
    @Transactional
    @Query("UPDATE TrustedDevice td SET td.isActive = false WHERE td.expiresAt < :expiredBefore")
    void deactivateExpiredDevices(@Param("expiredBefore") LocalDateTime expiredBefore);
    
    @Query("SELECT td FROM TrustedDevice td WHERE td.expiresAt BETWEEN :now AND :warningThreshold AND td.isActive = true")
    List<TrustedDevice> findDevicesNearExpiry(@Param("now") LocalDateTime now, 
                                             @Param("warningThreshold") LocalDateTime warningThreshold);
}