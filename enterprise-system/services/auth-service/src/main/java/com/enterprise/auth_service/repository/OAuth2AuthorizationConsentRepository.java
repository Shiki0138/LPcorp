package com.enterprise.auth_service.repository;

import com.enterprise.auth_service.entity.OAuth2AuthorizationConsent;
import com.enterprise.auth_service.entity.OAuth2AuthorizationConsentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository for OAuth2AuthorizationConsent entities
 */
@Repository
public interface OAuth2AuthorizationConsentRepository 
    extends JpaRepository<OAuth2AuthorizationConsent, OAuth2AuthorizationConsentId> {

    Optional<OAuth2AuthorizationConsent> findByRegisteredClientIdAndPrincipalName(
        String registeredClientId, String principalName);

    @Modifying
    @Query("DELETE FROM OAuth2AuthorizationConsent c WHERE c.expiresAt < :now")
    void deleteExpiredConsents(@Param("now") Instant now);

    void deleteByRegisteredClientIdAndPrincipalName(String registeredClientId, String principalName);
}