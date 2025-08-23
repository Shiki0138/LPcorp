package com.enterprise.auth_service;

import com.enterprise.auth_service.repository.OAuth2ClientRepository;
import com.enterprise.auth_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OAuth2 Authorization Server
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
public class OAuth2AuthorizationServerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private OAuth2ClientRepository clientRepository;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @Test
    public void contextLoads() {
        // Test that application context loads successfully
    }

    @Test
    public void testJwksEndpoint() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        
        mockMvc.perform(get("/oauth2/jwks"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.keys").isArray());
    }

    @Test
    public void testOpenIdConfigurationEndpoint() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        
        mockMvc.perform(get("/.well-known/openid_configuration"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.issuer").exists())
                .andExpect(jsonPath("$.authorization_endpoint").exists())
                .andExpect(jsonPath("$.token_endpoint").exists())
                .andExpect(jsonPath("$.jwks_uri").exists());
    }

    @Test
    public void testOAuthAuthorizationServerMetadata() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        
        mockMvc.perform(get("/.well-known/oauth-authorization-server"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.issuer").exists())
                .andExpect(jsonPath("$.grant_types_supported").isArray())
                .andExpect(jsonPath("$.response_types_supported").isArray());
    }

    @Test
    public void testAuthorizationEndpointRequiresAuthentication() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        
        mockMvc.perform(get("/oauth2/authorize")
                .param("response_type", "code")
                .param("client_id", "web-client")
                .param("redirect_uri", "http://localhost:3000/callback")
                .param("scope", "openid profile")
                .param("state", "test-state"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    public void testTokenEndpointRequiresClientAuthentication() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        
        mockMvc.perform(post("/oauth2/token")
                .contentType("application/x-www-form-urlencoded")
                .param("grant_type", "authorization_code")
                .param("code", "invalid-code")
                .param("redirect_uri", "http://localhost:3000/callback"))
                .andExpect(status().isUnauthorized());
    }
}