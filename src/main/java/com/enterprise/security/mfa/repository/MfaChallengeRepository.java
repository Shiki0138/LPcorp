package com.enterprise.security.mfa.repository;

import com.enterprise.security.mfa.model.MfaChallenge;
import com.enterprise.security.mfa.model.MfaType;
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
public interface MfaChallengeRepository extends JpaRepository<MfaChallenge, UUID> {
    
    Optional<MfaChallenge> findByIdAndIsUsedFalse(UUID id);
    
    Optional<MfaChallenge> findByUserIdAndSessionIdAndIsUsedFalse(String userId, String sessionId);
    
    List<MfaChallenge> findByUserIdAndTypeAndIsUsedFalse(String userId, MfaType type);
    
    @Query("SELECT mc FROM MfaChallenge mc WHERE mc.userId = :userId AND mc.deviceId = :deviceId AND mc.isUsed = false AND mc.expiresAt > :now ORDER BY mc.createdAt DESC")
    List<MfaChallenge> findActiveChallenge(@Param("userId") String userId, 
                                          @Param("deviceId") UUID deviceId, 
                                          @Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(mc) FROM MfaChallenge mc WHERE mc.userId = :userId AND mc.ipAddress = :ipAddress AND mc.createdAt > :since")
    long countChallengesByUserAndIpSince(@Param("userId") String userId, 
                                        @Param("ipAddress") String ipAddress, 
                                        @Param("since") LocalDateTime since);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM MfaChallenge mc WHERE mc.expiresAt < :expiredBefore")
    void deleteExpiredChallenges(@Param("expiredBefore") LocalDateTime expiredBefore);
    
    @Modifying
    @Transactional
    @Query("UPDATE MfaChallenge mc SET mc.isUsed = true, mc.usedAt = :usedAt WHERE mc.userId = :userId AND mc.sessionId = :sessionId AND mc.isUsed = false")
    void markChallengesAsUsed(@Param("userId") String userId, 
                             @Param("sessionId") String sessionId, 
                             @Param("usedAt") LocalDateTime usedAt);
}