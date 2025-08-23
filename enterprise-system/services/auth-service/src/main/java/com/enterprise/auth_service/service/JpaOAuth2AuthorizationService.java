package com.enterprise.auth_service.service;

import com.enterprise.auth_service.entity.OAuth2Authorization;
import com.enterprise.auth_service.repository.OAuth2AuthorizationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.service.OAuth2AuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;

/**
 * JPA-based implementation of OAuth2AuthorizationService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JpaOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private final OAuth2AuthorizationRepository authorizationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void save(OAuth2Authorization authorization) {
        log.debug("Saving OAuth2 authorization: {}", authorization.getId());
        
        com.enterprise.auth_service.entity.OAuth2Authorization entity = toEntity(authorization);
        authorizationRepository.save(entity);
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        log.debug("Removing OAuth2 authorization: {}", authorization.getId());
        authorizationRepository.deleteById(authorization.getId());
    }

    @Override
    public OAuth2Authorization findById(String id) {
        log.debug("Finding OAuth2 authorization by id: {}", id);
        return authorizationRepository.findById(id)
            .map(this::toObject)
            .orElse(null);
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        log.debug("Finding OAuth2 authorization by token type: {}", tokenType != null ? tokenType.getValue() : "null");
        
        com.enterprise.auth_service.entity.OAuth2Authorization result = null;
        
        if (tokenType == null) {
            result = authorizationRepository.findByState(token)
                .or(() -> authorizationRepository.findByAuthorizationCodeValue(token))
                .or(() -> authorizationRepository.findByAccessTokenValue(token))
                .or(() -> authorizationRepository.findByRefreshTokenValue(token))
                .or(() -> authorizationRepository.findByOidcIdTokenValue(token))
                .or(() -> authorizationRepository.findByUserCodeValue(token))
                .or(() -> authorizationRepository.findByDeviceCodeValue(token))
                .orElse(null);
        } else if (OAuth2ParameterNames.STATE.equals(tokenType.getValue())) {
            result = authorizationRepository.findByState(token).orElse(null);
        } else if (OAuth2ParameterNames.CODE.equals(tokenType.getValue())) {
            result = authorizationRepository.findByAuthorizationCodeValue(token).orElse(null);
        } else if (OAuth2TokenType.ACCESS_TOKEN.equals(tokenType)) {
            result = authorizationRepository.findByAccessTokenValue(token).orElse(null);
        } else if (OAuth2TokenType.REFRESH_TOKEN.equals(tokenType)) {
            result = authorizationRepository.findByRefreshTokenValue(token).orElse(null);
        } else if (OidcParameterNames.ID_TOKEN.equals(tokenType.getValue())) {
            result = authorizationRepository.findByOidcIdTokenValue(token).orElse(null);
        } else if ("user_code".equals(tokenType.getValue())) {
            result = authorizationRepository.findByUserCodeValue(token).orElse(null);
        } else if ("device_code".equals(tokenType.getValue())) {
            result = authorizationRepository.findByDeviceCodeValue(token).orElse(null);
        }

        return result != null ? toObject(result) : null;
    }

    private com.enterprise.auth_service.entity.OAuth2Authorization toEntity(OAuth2Authorization authorization) {
        com.enterprise.auth_service.entity.OAuth2Authorization.OAuth2AuthorizationBuilder builder = 
            com.enterprise.auth_service.entity.OAuth2Authorization.builder()
                .id(authorization.getId())
                .registeredClientId(authorization.getRegisteredClientId())
                .principalName(authorization.getPrincipalName())
                .authorizationGrantType(authorization.getAuthorizationGrantType().getValue())
                .authorizedScopes(StringUtils.collectionToDelimitedString(authorization.getAuthorizedScopes(), ","))
                .attributes(writeMap(authorization.getAttributes()))
                .state(authorization.getAttribute(OAuth2ParameterNames.STATE));

        OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode = 
            authorization.getToken(OAuth2AuthorizationCode.class);
        setTokenValues(authorizationCode, builder::authorizationCodeValue,
            builder::authorizationCodeIssuedAt, builder::authorizationCodeExpiresAt,
            builder::authorizationCodeMetadata);

        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = 
            authorization.getToken(OAuth2AccessToken.class);
        setTokenValues(accessToken, builder::accessTokenValue,
            builder::accessTokenIssuedAt, builder::accessTokenExpiresAt,
            builder::accessTokenMetadata);
        if (accessToken != null && accessToken.getToken().getScopes() != null) {
            builder.accessTokenScopes(StringUtils.collectionToDelimitedString(accessToken.getToken().getScopes(), ","));
            builder.accessTokenType(accessToken.getToken().getTokenType().getValue());
        }

        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = 
            authorization.getToken(OAuth2RefreshToken.class);
        setTokenValues(refreshToken, builder::refreshTokenValue,
            builder::refreshTokenIssuedAt, builder::refreshTokenExpiresAt,
            builder::refreshTokenMetadata);

        OAuth2Authorization.Token<OidcIdToken> oidcIdToken = 
            authorization.getToken(OidcIdToken.class);
        setTokenValues(oidcIdToken, builder::oidcIdTokenValue,
            builder::oidcIdTokenIssuedAt, builder::oidcIdTokenExpiresAt,
            builder::oidcIdTokenMetadata);

        return builder.build();
    }

    private OAuth2Authorization toObject(com.enterprise.auth_service.entity.OAuth2Authorization entity) {
        OAuth2Authorization.Builder builder = OAuth2Authorization.withRegisteredClientId(entity.getRegisteredClientId())
            .id(entity.getId())
            .principalName(entity.getPrincipalName())
            .authorizationGrantType(resolveAuthorizationGrantType(entity.getAuthorizationGrantType()))
            .authorizedScopes(StringUtils.commaDelimitedListToSet(entity.getAuthorizedScopes()))
            .attributes(attributes -> attributes.putAll(parseMap(entity.getAttributes())));

        if (entity.getState() != null) {
            builder.attribute(OAuth2ParameterNames.STATE, entity.getState());
        }

        if (entity.getAuthorizationCodeValue() != null) {
            OAuth2AuthorizationCode authorizationCode = new OAuth2AuthorizationCode(
                entity.getAuthorizationCodeValue(),
                entity.getAuthorizationCodeIssuedAt(),
                entity.getAuthorizationCodeExpiresAt());
            builder.token(authorizationCode, metadata -> 
                metadata.putAll(parseMap(entity.getAuthorizationCodeMetadata())));
        }

        if (entity.getAccessTokenValue() != null) {
            OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                entity.getAccessTokenValue(),
                entity.getAccessTokenIssuedAt(),
                entity.getAccessTokenExpiresAt(),
                StringUtils.commaDelimitedListToSet(entity.getAccessTokenScopes()));
            builder.token(accessToken, metadata -> 
                metadata.putAll(parseMap(entity.getAccessTokenMetadata())));
        }

        if (entity.getRefreshTokenValue() != null) {
            OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
                entity.getRefreshTokenValue(),
                entity.getRefreshTokenIssuedAt(),
                entity.getRefreshTokenExpiresAt());
            builder.token(refreshToken, metadata -> 
                metadata.putAll(parseMap(entity.getRefreshTokenMetadata())));
        }

        if (entity.getOidcIdTokenValue() != null) {
            OidcIdToken idToken = new OidcIdToken(
                entity.getOidcIdTokenValue(),
                entity.getOidcIdTokenIssuedAt(),
                entity.getOidcIdTokenExpiresAt(),
                parseMap(entity.getOidcIdTokenMetadata()));
            builder.token(idToken, metadata -> 
                metadata.putAll(parseMap(entity.getOidcIdTokenMetadata())));
        }

        return builder.build();
    }

    private void setTokenValues(OAuth2Authorization.Token<?> token,
                               Consumer<String> tokenValueConsumer,
                               Consumer<Instant> issuedAtConsumer,
                               Consumer<Instant> expiresAtConsumer,
                               Consumer<String> metadataConsumer) {
        if (token != null) {
            OAuth2Token oAuth2Token = token.getToken();
            tokenValueConsumer.accept(oAuth2Token.getTokenValue());
            issuedAtConsumer.accept(oAuth2Token.getIssuedAt());
            expiresAtConsumer.accept(oAuth2Token.getExpiresAt());
            metadataConsumer.accept(writeMap(token.getMetadata()));
        }
    }

    private Map<String, Object> parseMap(String data) {
        try {
            return StringUtils.hasText(data) ? 
                objectMapper.readValue(data, new TypeReference<Map<String, Object>>() {}) : 
                Map.of();
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    private String writeMap(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    private static AuthorizationGrantType resolveAuthorizationGrantType(String authorizationGrantType) {
        if (AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equals(authorizationGrantType)) {
            return AuthorizationGrantType.AUTHORIZATION_CODE;
        } else if (AuthorizationGrantType.CLIENT_CREDENTIALS.getValue().equals(authorizationGrantType)) {
            return AuthorizationGrantType.CLIENT_CREDENTIALS;
        } else if (AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(authorizationGrantType)) {
            return AuthorizationGrantType.REFRESH_TOKEN;
        } else if (AuthorizationGrantType.DEVICE_CODE.getValue().equals(authorizationGrantType)) {
            return AuthorizationGrantType.DEVICE_CODE;
        }
        return new AuthorizationGrantType(authorizationGrantType);
    }

}