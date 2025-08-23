package com.enterprise.auth_service.repository;

import com.enterprise.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository for User entities
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameAndIsEnabledTrue(String username);

    Optional<User> findByEmailAndIsEnabledTrue(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0 WHERE u.username = :username")
    void resetFailedLoginAttempts(@Param("username") String username);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.username = :username")
    void incrementFailedLoginAttempts(@Param("username") String username);

    @Modifying
    @Query("UPDATE User u SET u.isAccountNonLocked = false, u.accountLockedAt = :lockTime WHERE u.username = :username")
    void lockAccount(@Param("username") String username, @Param("lockTime") Instant lockTime);

    @Modifying
    @Query("UPDATE User u SET u.isAccountNonLocked = true, u.accountLockedAt = null, u.failedLoginAttempts = 0 WHERE u.username = :username")
    void unlockAccount(@Param("username") String username);

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime, u.lastLoginIp = :ipAddress, u.lastLoginUserAgent = :userAgent WHERE u.username = :username")
    void updateLastLogin(@Param("username") String username, @Param("loginTime") Instant loginTime, 
                        @Param("ipAddress") String ipAddress, @Param("userAgent") String userAgent);
}