package com.enterprise.auth_service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * OAuth2/OpenID Connect Discovery Controller (RFC 8414, RFC 5785)
 */
@RestController
@RequestMapping("/.well-known")
@RequiredArgsConstructor
@Slf4j
public class OAuth2DiscoveryController {

    @Value("${spring.security.oauth2.authorizationserver.issuer:http://localhost:8086}")
    private String issuer;

    @GetMapping(value = "/openid_configuration", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> openidConfiguration() {
        Map<String, Object> configuration = Map.of(
            "issuer", issuer,
            "authorization_endpoint", issuer + "/oauth2/authorize",
            "token_endpoint", issuer + "/oauth2/token",
            "token_endpoint_auth_methods_supported", List.of(
                "client_secret_basic",
                "client_secret_post",
                "none"
            ),
            "jwks_uri", issuer + "/oauth2/jwks",
            "userinfo_endpoint", issuer + "/userinfo",
            "introspection_endpoint", issuer + "/oauth2/introspect",
            "revocation_endpoint", issuer + "/oauth2/revoke",
            "device_authorization_endpoint", issuer + "/oauth2/device_authorization",
            "response_types_supported", List.of(
                "code"
            ),
            "grant_types_supported", List.of(
                "authorization_code",
                "client_credentials",
                "refresh_token",
                "urn:ietf:params:oauth:grant-type:device_code"
            ),
            "scopes_supported", List.of(
                "openid",
                "profile",
                "email",
                "read",
                "write",
                "admin",
                "service"
            ),
            "subject_types_supported", List.of(
                "public"
            ),
            "id_token_signing_alg_values_supported", List.of(
                "RS256"
            ),
            "token_endpoint_auth_signing_alg_values_supported", List.of(
                "RS256"
            ),
            "code_challenge_methods_supported", List.of(
                "S256",
                "plain"
            ),
            "claims_supported", List.of(
                "sub",
                "aud",
                "exp",
                "iat",
                "iss",
                "jti",
                "name",
                "given_name",
                "family_name",
                "email",
                "email_verified",
                "preferred_username"
            ),
            "response_modes_supported", List.of(
                "query",
                "fragment",
                "form_post"
            ),
            "authorization_response_iss_parameter_supported", true,
            "backchannel_logout_supported", false,
            "frontchannel_logout_supported", false,
            "request_object_signing_alg_values_supported", List.of(
                "RS256",
                "ES256"
            ),
            "request_parameter_supported", false,
            "request_uri_parameter_supported", false,
            "require_request_uri_registration", false,
            "claims_parameter_supported", false,
            "revocation_endpoint_auth_methods_supported", List.of(
                "client_secret_basic",
                "client_secret_post"
            ),
            "introspection_endpoint_auth_methods_supported", List.of(
                "client_secret_basic",
                "client_secret_post"
            ),
            "device_authorization_endpoint_auth_methods_supported", List.of(
                "none"
            )
        );

        return ResponseEntity.ok(configuration);
    }

    @GetMapping(value = "/oauth-authorization-server", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> oauthAuthorizationServer() {
        // OAuth 2.0 Authorization Server Metadata (RFC 8414)
        Map<String, Object> metadata = Map.of(
            "issuer", issuer,
            "authorization_endpoint", issuer + "/oauth2/authorize",
            "token_endpoint", issuer + "/oauth2/token",
            "token_endpoint_auth_methods_supported", List.of(
                "client_secret_basic",
                "client_secret_post",
                "none"
            ),
            "token_endpoint_auth_signing_alg_values_supported", List.of(
                "RS256"
            ),
            "jwks_uri", issuer + "/oauth2/jwks",
            "registration_endpoint", issuer + "/api/oauth2/clients",
            "scopes_supported", List.of(
                "openid",
                "profile",
                "email",
                "read",
                "write",
                "admin",
                "service"
            ),
            "response_types_supported", List.of(
                "code"
            ),
            "response_modes_supported", List.of(
                "query",
                "fragment",
                "form_post"
            ),
            "grant_types_supported", List.of(
                "authorization_code",
                "client_credentials", 
                "refresh_token",
                "urn:ietf:params:oauth:grant-type:device_code"
            ),
            "code_challenge_methods_supported", List.of(
                "S256",
                "plain"
            ),
            "introspection_endpoint", issuer + "/oauth2/introspect",
            "introspection_endpoint_auth_methods_supported", List.of(
                "client_secret_basic",
                "client_secret_post"
            ),
            "introspection_endpoint_auth_signing_alg_values_supported", List.of(
                "RS256"
            ),
            "revocation_endpoint", issuer + "/oauth2/revoke",
            "revocation_endpoint_auth_methods_supported", List.of(
                "client_secret_basic",
                "client_secret_post"
            ),
            "revocation_endpoint_auth_signing_alg_values_supported", List.of(
                "RS256"
            ),
            "device_authorization_endpoint", issuer + "/oauth2/device_authorization",
            "device_authorization_endpoint_auth_methods_supported", List.of(
                "none"
            )
        );

        return ResponseEntity.ok(metadata);
    }
}