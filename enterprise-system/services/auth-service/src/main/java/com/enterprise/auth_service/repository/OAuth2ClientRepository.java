package com.enterprise.auth_service.repository;

import com.enterprise.auth_service.entity.OAuth2Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for OAuth2Client entities
 */
@Repository
public interface OAuth2ClientRepository extends JpaRepository<OAuth2Client, String> {

    Optional<OAuth2Client> findByClientIdAndIsActiveTrue(String clientId);

    @Query("SELECT c FROM OAuth2Client c WHERE c.clientId = :clientId AND c.isActive = true")
    Optional<OAuth2Client> findActiveClient(@Param("clientId") String clientId);

    boolean existsByClientIdAndIsActiveTrue(String clientId);
}