package com.enterprise.auth_service.repository;

import com.enterprise.auth_service.entity.RsaKeyPair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for RSA key pair operations
 */
@Repository
public interface RsaKeyPairRepository extends JpaRepository<RsaKeyPair, Long> {

    Optional<RsaKeyPair> findByKeyId(String keyId);

    List<RsaKeyPair> findByActiveTrue();

    @Query("SELECT r FROM RsaKeyPair r WHERE r.active = true ORDER BY r.createdAt DESC")
    List<RsaKeyPair> findActiveKeysOrderedByCreatedDesc();

    @Query("SELECT r FROM RsaKeyPair r WHERE r.expiresAt <= :now")
    List<RsaKeyPair> findExpiredKeys(@Param("now") Instant now);

    @Query("SELECT r FROM RsaKeyPair r WHERE r.active = true AND r.expiresAt > :now ORDER BY r.createdAt DESC")
    List<RsaKeyPair> findActiveNonExpiredKeys(@Param("now") Instant now);

    @Modifying
    @Query("UPDATE RsaKeyPair r SET r.active = false, r.deactivatedAt = :deactivatedAt WHERE r.keyId = :keyId")
    int deactivateKey(@Param("keyId") String keyId, @Param("deactivatedAt") Instant deactivatedAt);

    @Modifying
    @Query("UPDATE RsaKeyPair r SET r.active = false, r.deactivatedAt = :deactivatedAt WHERE r.active = true")
    int deactivateAllKeys(@Param("deactivatedAt") Instant deactivatedAt);

    @Query("SELECT COUNT(r) FROM RsaKeyPair r WHERE r.active = true")
    int countActiveKeys();

    @Query("SELECT r FROM RsaKeyPair r WHERE r.active = true AND r.expiresAt > :now ORDER BY r.activatedAt ASC")
    Optional<RsaKeyPair> findOldestActiveKey(@Param("now") Instant now);

    @Query("SELECT r FROM RsaKeyPair r WHERE r.active = true AND r.expiresAt > :now ORDER BY r.activatedAt DESC")
    Optional<RsaKeyPair> findNewestActiveKey(@Param("now") Instant now);
}