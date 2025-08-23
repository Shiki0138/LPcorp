package com.enterprise.auth_service.repository;

import com.enterprise.auth_service.entity.OAuth2Authorization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for OAuth2Authorization entities
 */
@Repository
public interface OAuth2AuthorizationRepository extends JpaRepository<OAuth2Authorization, String> {

    Optional<OAuth2Authorization> findByState(String state);

    Optional<OAuth2Authorization> findByAuthorizationCodeValue(String authorizationCode);

    Optional<OAuth2Authorization> findByAccessTokenValue(String accessToken);

    Optional<OAuth2Authorization> findByRefreshTokenValue(String refreshToken);

    Optional<OAuth2Authorization> findByOidcIdTokenValue(String idToken);

    Optional<OAuth2Authorization> findByUserCodeValue(String userCode);

    Optional<OAuth2Authorization> findByDeviceCodeValue(String deviceCode);

    List<OAuth2Authorization> findByPrincipalNameAndRegisteredClientId(String principalName, String clientId);

    @Modifying
    @Query("DELETE FROM OAuth2Authorization a WHERE a.accessTokenExpiresAt < :now OR a.refreshTokenExpiresAt < :now")
    void deleteExpiredTokens(@Param("now") Instant now);

    @Query("SELECT COUNT(a) FROM OAuth2Authorization a WHERE a.registeredClientId = :clientId AND a.createdAt > :since")
    long countByClientIdSince(@Param("clientId") String clientId, @Param("since") Instant since);
}