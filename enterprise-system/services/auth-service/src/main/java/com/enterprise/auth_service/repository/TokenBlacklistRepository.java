package com.enterprise.auth_service.repository;

import com.enterprise.auth_service.entity.JwtToken;
import com.enterprise.auth_service.entity.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for token blacklist operations
 */
@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Long> {

    Optional<TokenBlacklist> findByTokenId(String tokenId);

    boolean existsByTokenId(String tokenId);

    List<TokenBlacklist> findByUserId(String userId);

    List<TokenBlacklist> findByUserIdAndTokenType(String userId, JwtToken.TokenType tokenType);

    @Query("SELECT b FROM TokenBlacklist b WHERE b.expiresAt <= :now")
    List<TokenBlacklist> findExpiredEntries(@Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM TokenBlacklist b WHERE b.expiresAt <= :cutoffDate")
    int deleteExpiredEntries(@Param("cutoffDate") Instant cutoffDate);

    @Query("SELECT COUNT(b) FROM TokenBlacklist b WHERE b.userId = :userId AND b.revokedAt >= :since")
    int countRecentRevocationsByUser(@Param("userId") String userId, @Param("since") Instant since);

    @Query("SELECT b FROM TokenBlacklist b WHERE b.revokedAt >= :since ORDER BY b.revokedAt DESC")
    List<TokenBlacklist> findRecentRevocations(@Param("since") Instant since);

    @Query("SELECT b.userId, COUNT(b) FROM TokenBlacklist b WHERE b.revokedAt >= :since GROUP BY b.userId HAVING COUNT(b) > :threshold")
    List<Object[]> findUsersWithExcessiveRevocations(@Param("since") Instant since, @Param("threshold") long threshold);
}