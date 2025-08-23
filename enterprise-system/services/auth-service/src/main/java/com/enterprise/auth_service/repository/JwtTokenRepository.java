package com.enterprise.auth_service.repository;

import com.enterprise.auth_service.entity.JwtToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for JWT token operations
 */
@Repository
public interface JwtTokenRepository extends JpaRepository<JwtToken, Long> {

    Optional<JwtToken> findByTokenId(String tokenId);

    List<JwtToken> findByUserIdAndTokenTypeAndRevokedFalse(String userId, JwtToken.TokenType tokenType);

    List<JwtToken> findByUserIdAndRevokedFalse(String userId);

    List<JwtToken> findByClientIdAndRevokedFalse(String clientId);

    @Query("SELECT t FROM JwtToken t WHERE t.expiresAt <= :now AND t.revoked = false")
    List<JwtToken> findExpiredTokens(@Param("now") Instant now);

    @Modifying
    @Query("UPDATE JwtToken t SET t.revoked = true, t.revokedAt = :revokedAt, t.revokedReason = :reason WHERE t.tokenId = :tokenId")
    int revokeToken(@Param("tokenId") String tokenId, @Param("revokedAt") Instant revokedAt, @Param("reason") String reason);

    @Modifying
    @Query("UPDATE JwtToken t SET t.revoked = true, t.revokedAt = :revokedAt, t.revokedReason = :reason WHERE t.userId = :userId AND t.revoked = false")
    int revokeAllUserTokens(@Param("userId") String userId, @Param("revokedAt") Instant revokedAt, @Param("reason") String reason);

    @Modifying
    @Query("UPDATE JwtToken t SET t.lastUsedAt = :lastUsedAt WHERE t.tokenId = :tokenId")
    int updateLastUsedAt(@Param("tokenId") String tokenId, @Param("lastUsedAt") Instant lastUsedAt);

    @Modifying
    @Query("DELETE FROM JwtToken t WHERE t.expiresAt <= :cutoffDate")
    int deleteExpiredTokens(@Param("cutoffDate") Instant cutoffDate);

    @Query("SELECT COUNT(t) FROM JwtToken t WHERE t.userId = :userId AND t.tokenType = :tokenType AND t.revoked = false AND t.expiresAt > :now")
    int countActiveTokensByUserAndType(@Param("userId") String userId, @Param("tokenType") JwtToken.TokenType tokenType, @Param("now") Instant now);

    @Query("SELECT t FROM JwtToken t WHERE t.userId = :userId AND t.tokenType = 'REFRESH_TOKEN' AND t.revoked = false AND t.expiresAt > :now ORDER BY t.createdAt DESC")
    List<JwtToken> findActiveRefreshTokensByUser(@Param("userId") String userId, @Param("now") Instant now);
}