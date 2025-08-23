package com.enterprise.auth_service.controller;

import com.enterprise.auth_service.dto.ClientRegistrationRequest;
import com.enterprise.auth_service.dto.ClientRegistrationResponse;
import com.enterprise.auth_service.service.ClientManagementService;
import com.enterprise.auth_service.service.SecurityEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

/**
 * OAuth2 Client Management Controller
 */
@RestController
@RequestMapping("/api/oauth2/clients")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ClientManagementController {

    private final ClientManagementService clientManagementService;
    private final SecurityEventService securityEventService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClientRegistrationResponse> registerClient(
            @Valid @RequestBody ClientRegistrationRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            ClientRegistrationResponse response = clientManagementService.registerClient(request);
            
            securityEventService.logSecurityEvent("CLIENT_REGISTERED", null, response.getClientId(), 
                ipAddress, httpRequest.getHeader("User-Agent"), true, null, null);
            
            log.info("OAuth2 client registered: {}", response.getClientId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("Error registering OAuth2 client", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClientRegistrationResponse>> getAllClients() {
        try {
            List<ClientRegistrationResponse> clients = clientManagementService.getAllClients();
            return ResponseEntity.ok(clients);
        } catch (Exception e) {
            log.error("Error retrieving OAuth2 clients", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{clientId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClientRegistrationResponse> getClient(@PathVariable String clientId) {
        try {
            ClientRegistrationResponse client = clientManagementService.getClient(clientId);
            if (client == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(client);
        } catch (Exception e) {
            log.error("Error retrieving OAuth2 client: {}", clientId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{clientId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClientRegistrationResponse> updateClient(
            @PathVariable String clientId,
            @Valid @RequestBody ClientRegistrationRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            ClientRegistrationResponse response = clientManagementService.updateClient(clientId, request);
            
            securityEventService.logSecurityEvent("CLIENT_UPDATED", null, clientId, 
                ipAddress, httpRequest.getHeader("User-Agent"), true, null, null);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating OAuth2 client: {}", clientId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{clientId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteClient(
            @PathVariable String clientId,
            HttpServletRequest httpRequest) {
        
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            clientManagementService.deleteClient(clientId);
            
            securityEventService.logSecurityEvent("CLIENT_DELETED", null, clientId, 
                ipAddress, httpRequest.getHeader("User-Agent"), true, null, null);
            
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting OAuth2 client: {}", clientId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{clientId}/regenerate-secret")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClientRegistrationResponse> regenerateClientSecret(
            @PathVariable String clientId,
            HttpServletRequest httpRequest) {
        
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            ClientRegistrationResponse response = clientManagementService.regenerateClientSecret(clientId);
            
            securityEventService.logSecurityEvent("CLIENT_SECRET_REGENERATED", null, clientId, 
                ipAddress, httpRequest.getHeader("User-Agent"), true, null, null);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error regenerating client secret for: {}", clientId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{clientId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activateClient(
            @PathVariable String clientId,
            HttpServletRequest httpRequest) {
        
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            clientManagementService.activateClient(clientId);
            
            securityEventService.logSecurityEvent("CLIENT_ACTIVATED", null, clientId, 
                ipAddress, httpRequest.getHeader("User-Agent"), true, null, null);
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error activating OAuth2 client: {}", clientId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{clientId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateClient(
            @PathVariable String clientId,
            HttpServletRequest httpRequest) {
        
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            clientManagementService.deactivateClient(clientId);
            
            securityEventService.logSecurityEvent("CLIENT_DEACTIVATED", null, clientId, 
                ipAddress, httpRequest.getHeader("User-Agent"), true, null, null);
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deactivating OAuth2 client: {}", clientId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}