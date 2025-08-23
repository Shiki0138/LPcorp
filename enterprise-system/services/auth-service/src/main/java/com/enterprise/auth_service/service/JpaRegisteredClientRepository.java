package com.enterprise.auth_service.service;

import com.enterprise.auth_service.entity.OAuth2Client;
import com.enterprise.auth_service.repository.OAuth2ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JPA-based implementation of RegisteredClientRepository
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JpaRegisteredClientRepository implements RegisteredClientRepository {

    private final OAuth2ClientRepository clientRepository;

    @Override
    public void save(RegisteredClient registeredClient) {
        log.debug("Saving registered client: {}", registeredClient.getClientId());
        
        OAuth2Client client = OAuth2Client.builder()
            .clientId(registeredClient.getClientId())
            .clientSecret(registeredClient.getClientSecret())
            .clientName(registeredClient.getClientName())
            .redirectUris(registeredClient.getRedirectUris())
            .authorizedGrantTypes(registeredClient.getAuthorizationGrantTypes().stream()
                .map(AuthorizationGrantType::getValue)
                .collect(Collectors.toSet()))
            .scopes(registeredClient.getScopes())
            .accessTokenValiditySeconds(Math.toIntExact(
                registeredClient.getTokenSettings().getAccessTokenTimeToLive().getSeconds()))
            .refreshTokenValiditySeconds(Math.toIntExact(
                registeredClient.getTokenSettings().getRefreshTokenTimeToLive().getSeconds()))
            .requireAuthorizationConsent(registeredClient.getClientSettings().isRequireAuthorizationConsent())
            .requireProofKey(registeredClient.getClientSettings().isRequireProofKey())
            .clientAuthenticationMethods(registeredClient.getClientAuthenticationMethods().stream()
                .map(ClientAuthenticationMethod::getValue)
                .collect(Collectors.joining(",")))
            .authorizationCodeTimeToLive(Math.toIntExact(
                registeredClient.getTokenSettings().getAuthorizationCodeTimeToLive().getSeconds()))
            .deviceCodeTimeToLive(Math.toIntExact(
                registeredClient.getTokenSettings().getDeviceCodeTimeToLive().getSeconds()))
            .isActive(true)
            .build();

        clientRepository.save(client);
    }

    @Override
    public RegisteredClient findById(String id) {
        log.debug("Finding registered client by id: {}", id);
        return clientRepository.findById(id)
            .map(this::toRegisteredClient)
            .orElse(null);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        log.debug("Finding registered client by clientId: {}", clientId);
        return clientRepository.findByClientIdAndIsActiveTrue(clientId)
            .map(this::toRegisteredClient)
            .orElse(null);
    }

    private RegisteredClient toRegisteredClient(OAuth2Client client) {
        Set<AuthorizationGrantType> grantTypes = client.getAuthorizedGrantTypes().stream()
            .map(AuthorizationGrantType::new)
            .collect(Collectors.toSet());

        Set<ClientAuthenticationMethod> authMethods = StringUtils.hasText(client.getClientAuthenticationMethods())
            ? Arrays.stream(client.getClientAuthenticationMethods().split(","))
                .map(String::trim)
                .map(ClientAuthenticationMethod::new)
                .collect(Collectors.toSet())
            : Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);

        RegisteredClient.Builder builder = RegisteredClient.withId(client.getClientId())
            .clientId(client.getClientId())
            .clientSecret(client.getClientSecret())
            .clientName(client.getClientName() != null ? client.getClientName() : client.getClientId());

        // Add redirect URIs
        if (client.getRedirectUris() != null) {
            client.getRedirectUris().forEach(builder::redirectUri);
        }

        // Add authentication methods
        authMethods.forEach(builder::clientAuthenticationMethod);

        // Add grant types
        grantTypes.forEach(builder::authorizationGrantType);

        // Add scopes
        if (client.getScopes() != null) {
            client.getScopes().forEach(builder::scope);
        }

        // Client settings
        ClientSettings clientSettings = ClientSettings.builder()
            .requireAuthorizationConsent(client.getRequireAuthorizationConsent() != null 
                ? client.getRequireAuthorizationConsent() : false)
            .requireProofKey(client.getRequireProofKey() != null 
                ? client.getRequireProofKey() : false)
            .build();

        // Token settings
        TokenSettings tokenSettings = TokenSettings.builder()
            .accessTokenTimeToLive(Duration.ofSeconds(
                client.getAccessTokenValiditySeconds() != null 
                    ? client.getAccessTokenValiditySeconds() : 3600))
            .refreshTokenTimeToLive(Duration.ofSeconds(
                client.getRefreshTokenValiditySeconds() != null 
                    ? client.getRefreshTokenValiditySeconds() : 86400))
            .authorizationCodeTimeToLive(Duration.ofSeconds(
                client.getAuthorizationCodeTimeToLive() != null 
                    ? client.getAuthorizationCodeTimeToLive() : 600))
            .deviceCodeTimeToLive(Duration.ofSeconds(
                client.getDeviceCodeTimeToLive() != null 
                    ? client.getDeviceCodeTimeToLive() : 1800))
            .reuseRefreshTokens(false)
            .build();

        return builder
            .clientSettings(clientSettings)
            .tokenSettings(tokenSettings)
            .build();
    }
}