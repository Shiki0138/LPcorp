package com.enterprise.auth_service.service;

import com.enterprise.auth_service.dto.ClientRegistrationRequest;
import com.enterprise.auth_service.dto.ClientRegistrationResponse;
import com.enterprise.auth_service.entity.OAuth2Client;
import com.enterprise.auth_service.repository.OAuth2ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing OAuth2 clients
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ClientManagementService {

    private final OAuth2ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public ClientRegistrationResponse registerClient(ClientRegistrationRequest request) {
        log.info("Registering new OAuth2 client: {}", request.getClientName());

        // Generate client ID and secret
        String clientId = generateClientId();
        String clientSecret = generateClientSecret();

        OAuth2Client client = OAuth2Client.builder()
            .clientId(clientId)
            .clientSecret(passwordEncoder.encode(clientSecret))
            .clientName(request.getClientName())
            .redirectUris(request.getRedirectUris())
            .authorizedGrantTypes(request.getAuthorizedGrantTypes())
            .scopes(request.getScopes())
            .accessTokenValiditySeconds(request.getAccessTokenValiditySeconds() != null ? 
                request.getAccessTokenValiditySeconds() : 3600)
            .refreshTokenValiditySeconds(request.getRefreshTokenValiditySeconds() != null ? 
                request.getRefreshTokenValiditySeconds() : 86400)
            .requireAuthorizationConsent(request.getRequireAuthorizationConsent() != null ? 
                request.getRequireAuthorizationConsent() : false)
            .requireProofKey(request.getRequireProofKey() != null ? 
                request.getRequireProofKey() : false)
            .clientAuthenticationMethods(request.getClientAuthenticationMethods() != null ? 
                String.join(",", request.getClientAuthenticationMethods()) : "client_secret_basic")
            .authorizationCodeTimeToLive(request.getAuthorizationCodeTimeToLive() != null ? 
                request.getAuthorizationCodeTimeToLive() : 600)
            .deviceCodeTimeToLive(request.getDeviceCodeTimeToLive() != null ? 
                request.getDeviceCodeTimeToLive() : 1800)
            .isActive(true)
            .rateLimitPerHour(request.getRateLimitPerHour() != null ? 
                request.getRateLimitPerHour() : 1000)
            .description(request.getDescription())
            .contactEmail(request.getContactEmail())
            .websiteUrl(request.getWebsiteUrl())
            .build();

        OAuth2Client savedClient = clientRepository.save(client);

        return toResponse(savedClient, clientSecret, false);
    }

    @Transactional(readOnly = true)
    public List<ClientRegistrationResponse> getAllClients() {
        return clientRepository.findAll().stream()
            .map(client -> toResponse(client, null, false))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClientRegistrationResponse getClient(String clientId) {
        return clientRepository.findById(clientId)
            .map(client -> toResponse(client, null, false))
            .orElse(null);
    }

    public ClientRegistrationResponse updateClient(String clientId, ClientRegistrationRequest request) {
        log.info("Updating OAuth2 client: {}", clientId);

        OAuth2Client client = clientRepository.findById(clientId)
            .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        // Update fields
        client.setClientName(request.getClientName());
        client.setRedirectUris(request.getRedirectUris());
        client.setAuthorizedGrantTypes(request.getAuthorizedGrantTypes());
        client.setScopes(request.getScopes());
        
        if (request.getAccessTokenValiditySeconds() != null) {
            client.setAccessTokenValiditySeconds(request.getAccessTokenValiditySeconds());
        }
        
        if (request.getRefreshTokenValiditySeconds() != null) {
            client.setRefreshTokenValiditySeconds(request.getRefreshTokenValiditySeconds());
        }
        
        if (request.getRequireAuthorizationConsent() != null) {
            client.setRequireAuthorizationConsent(request.getRequireAuthorizationConsent());
        }
        
        if (request.getRequireProofKey() != null) {
            client.setRequireProofKey(request.getRequireProofKey());
        }
        
        if (request.getClientAuthenticationMethods() != null) {
            client.setClientAuthenticationMethods(String.join(",", request.getClientAuthenticationMethods()));
        }
        
        if (request.getAuthorizationCodeTimeToLive() != null) {
            client.setAuthorizationCodeTimeToLive(request.getAuthorizationCodeTimeToLive());
        }
        
        if (request.getDeviceCodeTimeToLive() != null) {
            client.setDeviceCodeTimeToLive(request.getDeviceCodeTimeToLive());
        }
        
        if (request.getRateLimitPerHour() != null) {
            client.setRateLimitPerHour(request.getRateLimitPerHour());
        }
        
        client.setDescription(request.getDescription());
        client.setContactEmail(request.getContactEmail());
        client.setWebsiteUrl(request.getWebsiteUrl());

        OAuth2Client savedClient = clientRepository.save(client);
        return toResponse(savedClient, null, false);
    }

    public void deleteClient(String clientId) {
        log.info("Deleting OAuth2 client: {}", clientId);
        
        OAuth2Client client = clientRepository.findById(clientId)
            .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));
        
        // Soft delete by deactivating
        client.setIsActive(false);
        clientRepository.save(client);
    }

    public ClientRegistrationResponse regenerateClientSecret(String clientId) {
        log.info("Regenerating client secret for: {}", clientId);

        OAuth2Client client = clientRepository.findById(clientId)
            .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        String newSecret = generateClientSecret();
        client.setClientSecret(passwordEncoder.encode(newSecret));
        
        OAuth2Client savedClient = clientRepository.save(client);
        return toResponse(savedClient, newSecret, true);
    }

    public void activateClient(String clientId) {
        log.info("Activating OAuth2 client: {}", clientId);
        
        OAuth2Client client = clientRepository.findById(clientId)
            .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));
        
        client.setIsActive(true);
        clientRepository.save(client);
    }

    public void deactivateClient(String clientId) {
        log.info("Deactivating OAuth2 client: {}", clientId);
        
        OAuth2Client client = clientRepository.findById(clientId)
            .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));
        
        client.setIsActive(false);
        clientRepository.save(client);
    }

    private String generateClientId() {
        // Generate a unique client ID
        String prefix = "client_";
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return prefix + uuid.substring(0, 16);
    }

    private String generateClientSecret() {
        // Generate a secure random client secret
        byte[] secretBytes = new byte[32];
        secureRandom.nextBytes(secretBytes);
        return "secret_" + java.util.Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(secretBytes);
    }

    private ClientRegistrationResponse toResponse(OAuth2Client client, String plainSecret, boolean secretRegenerated) {
        Set<String> authMethods = client.getClientAuthenticationMethods() != null ?
            Set.of(client.getClientAuthenticationMethods().split(",")) :
            Set.of("client_secret_basic");

        return ClientRegistrationResponse.builder()
            .clientId(client.getClientId())
            .clientSecret(plainSecret) // Only include if provided
            .clientName(client.getClientName())
            .redirectUris(client.getRedirectUris())
            .authorizedGrantTypes(client.getAuthorizedGrantTypes())
            .scopes(client.getScopes())
            .accessTokenValiditySeconds(client.getAccessTokenValiditySeconds())
            .refreshTokenValiditySeconds(client.getRefreshTokenValiditySeconds())
            .requireAuthorizationConsent(client.getRequireAuthorizationConsent())
            .requireProofKey(client.getRequireProofKey())
            .clientAuthenticationMethods(authMethods)
            .authorizationCodeTimeToLive(client.getAuthorizationCodeTimeToLive())
            .deviceCodeTimeToLive(client.getDeviceCodeTimeToLive())
            .isActive(client.getIsActive())
            .rateLimitPerHour(client.getRateLimitPerHour())
            .createdAt(client.getCreatedAt())
            .updatedAt(client.getUpdatedAt())
            .createdBy(client.getCreatedBy())
            .description(client.getDescription())
            .contactEmail(client.getContactEmail())
            .websiteUrl(client.getWebsiteUrl())
            .secretRegenerated(secretRegenerated)
            .build();
    }
}