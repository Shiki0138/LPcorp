package com.enterprise.security.mfa.service;

import com.enterprise.security.mfa.model.MfaDevice;
import com.enterprise.security.mfa.repository.MfaDeviceRepository;
import com.yubico.webauthn.*;
import com.yubico.webauthn.data.*;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebAuthnService {
    
    private final RelyingParty relyingParty;
    private final MfaDeviceRepository deviceRepository;
    private final Map<String, ByteArray> challengeStore = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Value("${app.mfa.webauthn.rp-id:localhost}")
    private String rpId;
    
    @Value("${app.mfa.webauthn.rp-name:Enterprise App}")
    private String rpName;
    
    @Value("${app.mfa.webauthn.challenge-timeout:300000}") // 5 minutes
    private long challengeTimeout;
    
    @Autowired
    public WebAuthnService(MfaDeviceRepository deviceRepository,
                          @Value("${app.mfa.webauthn.rp-id:localhost}") String rpId,
                          @Value("${app.mfa.webauthn.rp-name:Enterprise App}") String rpName) {
        this.deviceRepository = deviceRepository;
        this.relyingParty = RelyingParty.builder()
            .identity(RelyingPartyIdentity.builder()
                .id(rpId)
                .name(rpName)
                .build())
            .credentialRepository(new CredentialRepositoryImpl())
            .allowUntrustedAttestation(false)
            .allowOriginPort(true) // For development
            .build();
    }
    
    /**
     * Starts WebAuthn registration process
     */
    public PublicKeyCredentialCreationOptions startRegistration(String userId, String username, 
                                                               String displayName, boolean requireResidentKey) {
        UserIdentity user = UserIdentity.builder()
            .name(username)
            .displayName(displayName != null ? displayName : username)
            .id(new ByteArray(userId.getBytes()))
            .build();
        
        AuthenticatorSelectionCriteria.Builder selectionBuilder = AuthenticatorSelectionCriteria.builder()
            .userVerification(UserVerificationRequirement.PREFERRED);
        
        if (requireResidentKey) {
            selectionBuilder
                .residentKey(ResidentKeyRequirement.REQUIRED)
                .authenticatorAttachment(AuthenticatorAttachment.PLATFORM);
        } else {
            selectionBuilder
                .residentKey(ResidentKeyRequirement.DISCOURAGED)
                .authenticatorAttachment(AuthenticatorAttachment.CROSS_PLATFORM);
        }
        
        ByteArray challenge = generateChallenge();
        
        StartRegistrationOptions options = StartRegistrationOptions.builder()
            .user(user)
            .authenticatorSelection(selectionBuilder.build())
            .timeout(challengeTimeout)
            .attestation(AttestationConveyancePreference.DIRECT)
            .build();
        
        PublicKeyCredentialCreationOptions creationOptions = relyingParty.startRegistration(options);
        
        // Store challenge for verification
        storeChallenge(userId, creationOptions.getChallenge());
        
        return creationOptions;
    }
    
    /**
     * Finishes WebAuthn registration process
     */
    public RegistrationResult finishRegistration(String userId, String deviceName,
                                               PublicKeyCredential<AuthenticatorAttestationResponse> credential) 
            throws RegistrationFailedException {
        
        ByteArray challenge = getStoredChallenge(userId);
        if (challenge == null) {
            throw new RegistrationFailedException("No stored challenge found for user");
        }
        
        PublicKeyCredentialCreationOptions request = PublicKeyCredentialCreationOptions.builder()
            .challenge(challenge)
            .rp(relyingParty.getIdentity())
            .user(UserIdentity.builder()
                .name(userId)
                .displayName(userId)
                .id(new ByteArray(userId.getBytes()))
                .build())
            .pubKeyCredParams(Arrays.asList(
                PublicKeyCredentialParameters.builder()
                    .type(PublicKeyCredentialType.PUBLIC_KEY)
                    .alg(COSEAlgorithmIdentifier.ES256)
                    .build(),
                PublicKeyCredentialParameters.builder()
                    .type(PublicKeyCredentialType.PUBLIC_KEY)
                    .alg(COSEAlgorithmIdentifier.RS256)
                    .build()
            ))
            .build();
        
        FinishRegistrationOptions options = FinishRegistrationOptions.builder()
            .request(request)
            .response(credential)
            .build();
        
        RegistrationResult result = relyingParty.finishRegistration(options);
        
        if (result.isSuccess()) {
            // Save device to database
            MfaDevice device = new MfaDevice();
            device.setUserId(userId);
            device.setType(credential.getResponse().getParsedAuthenticatorData().getFlags().UV 
                ? com.enterprise.security.mfa.model.MfaType.WEBAUTHN_PLATFORM
                : com.enterprise.security.mfa.model.MfaType.WEBAUTHN_CROSS_PLATFORM);
            device.setDeviceName(deviceName);
            device.setDeviceIdentifier(credential.getId().getBase64Url());
            device.setPublicKey(result.getPublicKeyCose().getBase64Url());
            device.setSignatureCount(result.getSignatureCount());
            device.markAsVerified();
            
            deviceRepository.save(device);
            
            // Clean up challenge
            removeChallenge(userId);
        }
        
        return result;
    }
    
    /**
     * Starts WebAuthn authentication process
     */
    public PublicKeyCredentialRequestOptions startAuthentication(String userId) {
        List<MfaDevice> devices = deviceRepository.findByUserIdAndIsActiveTrue(userId);
        
        List<PublicKeyCredentialDescriptor> allowCredentials = new ArrayList<>();
        for (MfaDevice device : devices) {
            if (device.isWebAuthn() && device.isVerified()) {
                allowCredentials.add(PublicKeyCredentialDescriptor.builder()
                    .id(ByteArray.fromBase64Url(device.getDeviceIdentifier()))
                    .type(PublicKeyCredentialType.PUBLIC_KEY)
                    .build());
            }
        }
        
        ByteArray challenge = generateChallenge();
        
        StartAssertionOptions options = StartAssertionOptions.builder()
            .username(userId)
            .userVerification(UserVerificationRequirement.PREFERRED)
            .timeout(challengeTimeout)
            .build();
        
        PublicKeyCredentialRequestOptions requestOptions = relyingParty.startAssertion(options);
        
        // Store challenge for verification
        storeChallenge(userId, requestOptions.getChallenge());
        
        return requestOptions;
    }
    
    /**
     * Finishes WebAuthn authentication process
     */
    public AssertionResult finishAuthentication(String userId,
                                              PublicKeyCredential<AuthenticatorAssertionResponse> credential)
            throws AssertionFailedException {
        
        ByteArray challenge = getStoredChallenge(userId);
        if (challenge == null) {
            throw new AssertionFailedException("No stored challenge found for user");
        }
        
        PublicKeyCredentialRequestOptions request = PublicKeyCredentialRequestOptions.builder()
            .challenge(challenge)
            .rpId(relyingParty.getIdentity().getId())
            .allowCredentials(Arrays.asList(
                PublicKeyCredentialDescriptor.builder()
                    .id(credential.getId())
                    .type(PublicKeyCredentialType.PUBLIC_KEY)
                    .build()
            ))
            .build();
        
        FinishAssertionOptions options = FinishAssertionOptions.builder()
            .request(request)
            .response(credential)
            .build();
        
        AssertionResult result = relyingParty.finishAssertion(options);
        
        if (result.isSuccess()) {
            // Update signature count
            Optional<MfaDevice> deviceOpt = deviceRepository
                .findByDeviceIdentifierAndIsActiveTrue(credential.getId().getBase64Url());
            
            if (deviceOpt.isPresent()) {
                MfaDevice device = deviceOpt.get();
                device.setSignatureCount(result.getSignatureCount());
                device.markAsUsed();
                deviceRepository.save(device);
            }
            
            // Clean up challenge
            removeChallenge(userId);
        }
        
        return result;
    }
    
    /**
     * Gets user's WebAuthn devices
     */
    public List<MfaDevice> getUserWebAuthnDevices(String userId) {
        return deviceRepository.findByUserIdAndIsActiveTrue(userId)
            .stream()
            .filter(MfaDevice::isWebAuthn)
            .toList();
    }
    
    /**
     * Removes a WebAuthn device
     */
    public boolean removeDevice(String userId, String deviceId) {
        Optional<MfaDevice> deviceOpt = deviceRepository
            .findByUserIdAndDeviceIdentifierAndIsActiveTrue(userId, deviceId);
        
        if (deviceOpt.isPresent()) {
            MfaDevice device = deviceOpt.get();
            device.setActive(false);
            deviceRepository.save(device);
            return true;
        }
        
        return false;
    }
    
    // Private helper methods
    
    private ByteArray generateChallenge() {
        byte[] challengeBytes = new byte[32];
        secureRandom.nextBytes(challengeBytes);
        return new ByteArray(challengeBytes);
    }
    
    private void storeChallenge(String userId, ByteArray challenge) {
        challengeStore.put(userId, challenge);
        
        // Schedule cleanup
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                challengeStore.remove(userId);
            }
        }, challengeTimeout);
    }
    
    private ByteArray getStoredChallenge(String userId) {
        return challengeStore.get(userId);
    }
    
    private void removeChallenge(String userId) {
        challengeStore.remove(userId);
    }
    
    /**
     * Credential Repository Implementation for WebAuthn
     */
    private class CredentialRepositoryImpl implements CredentialRepository {
        
        @Override
        public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
            List<MfaDevice> devices = deviceRepository.findByUserIdAndIsActiveTrue(username);
            Set<PublicKeyCredentialDescriptor> credentials = new HashSet<>();
            
            for (MfaDevice device : devices) {
                if (device.isWebAuthn() && device.isVerified()) {
                    credentials.add(PublicKeyCredentialDescriptor.builder()
                        .id(ByteArray.fromBase64Url(device.getDeviceIdentifier()))
                        .type(PublicKeyCredentialType.PUBLIC_KEY)
                        .build());
                }
            }
            
            return credentials;
        }
        
        @Override
        public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
            // User handle is the user ID in our implementation
            return Optional.of(new String(userHandle.getBytes()));
        }
        
        @Override
        public Optional<ByteArray> getUserHandleForUsername(String username) {
            return Optional.of(new ByteArray(username.getBytes()));
        }
        
        @Override
        public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
            String userId = new String(userHandle.getBytes());
            Optional<MfaDevice> deviceOpt = deviceRepository
                .findByUserIdAndDeviceIdentifierAndIsActiveTrue(userId, credentialId.getBase64Url());
            
            if (deviceOpt.isPresent()) {
                MfaDevice device = deviceOpt.get();
                return Optional.of(RegisteredCredential.builder()
                    .credentialId(credentialId)
                    .userHandle(userHandle)
                    .publicKeyCose(ByteArray.fromBase64Url(device.getPublicKey()))
                    .signatureCount(device.getSignatureCount())
                    .build());
            }
            
            return Optional.empty();
        }
        
        @Override
        public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
            // Not implemented for this use case
            return Collections.emptySet();
        }
    }
}