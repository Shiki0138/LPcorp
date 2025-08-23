package com.enterprise.security.mfa.service;

import org.apache.commons.codec.binary.Base32;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.util.HashMap;
import java.util.Map;

@Service
public class TOTPService {
    
    private static final int SECRET_SIZE = 32;
    private static final int CODE_LENGTH = 6;
    private static final int TIME_STEP = 30; // seconds
    private static final int WINDOW_SIZE = 1; // Allow 1 window before/after for clock skew
    
    @Value("${app.mfa.totp.issuer:Enterprise App}")
    private String issuer;
    
    @Value("${app.mfa.totp.algorithm:HmacSHA256}")
    private String algorithm;
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Generates a new TOTP secret key
     */
    public String generateSecret() {
        byte[] buffer = new byte[SECRET_SIZE];
        secureRandom.nextBytes(buffer);
        return new Base32().encodeToString(buffer);
    }
    
    /**
     * Generates TOTP URI for QR code generation
     */
    public String generateTOTPUri(String email, String secret) {
        return generateTOTPUri(email, secret, issuer);
    }
    
    /**
     * Generates TOTP URI with custom issuer
     */
    public String generateTOTPUri(String email, String secret, String customIssuer) {
        return String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=%s&digits=%d&period=%d",
            customIssuer,
            email,
            secret,
            customIssuer,
            algorithm.replace("Hmac", ""),
            CODE_LENGTH,
            TIME_STEP
        );
    }
    
    /**
     * Generates QR code as Base64 encoded PNG image
     */
    public String generateQRCodeImage(String totpUri) throws WriterException, IOException {
        return generateQRCodeImage(totpUri, 300, 300);
    }
    
    /**
     * Generates QR code with custom dimensions
     */
    public String generateQRCodeImage(String totpUri, int width, int height) throws WriterException, IOException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);
        
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix bitMatrix = writer.encode(totpUri, BarcodeFormat.QR_CODE, width, height, hints);
        
        BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", outputStream);
        
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }
    
    /**
     * Verifies TOTP code with time window tolerance
     */
    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || code.length() != CODE_LENGTH) {
            return false;
        }
        
        long currentTime = getCurrentTimeWindow();
        
        // Check current time window and adjacent windows for clock skew
        for (int i = -WINDOW_SIZE; i <= WINDOW_SIZE; i++) {
            String calculatedCode = generateCode(secret, currentTime + i);
            if (calculatedCode != null && calculatedCode.equals(code)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Verifies TOTP code with replay attack protection
     */
    public boolean verifyCodeWithReplayProtection(String secret, String code, Long lastUsedTimeWindow) {
        if (!verifyCode(secret, code)) {
            return false;
        }
        
        long currentTimeWindow = getCurrentTimeWindow();
        
        // Prevent replay attacks - code must be from current or future time window
        if (lastUsedTimeWindow != null && currentTimeWindow <= lastUsedTimeWindow) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Gets current time window for replay protection
     */
    public long getCurrentTimeWindow() {
        return System.currentTimeMillis() / 1000 / TIME_STEP;
    }
    
    /**
     * Generates TOTP code for specific time window
     */
    public String generateCode(String secret, long timeWindow) {
        try {
            byte[] secretBytes = new Base32().decode(secret);
            byte[] timeBytes = ByteBuffer.allocate(8).putLong(timeWindow).array();
            
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secretBytes, algorithm));
            byte[] hash = mac.doFinal(timeBytes);
            
            int offset = hash[hash.length - 1] & 0xF;
            int binary = ((hash[offset] & 0x7F) << 24) |
                        ((hash[offset + 1] & 0xFF) << 16) |
                        ((hash[offset + 2] & 0xFF) << 8) |
                        (hash[offset + 3] & 0xFF);
            
            int otp = binary % (int) Math.pow(10, CODE_LENGTH);
            return String.format("%0" + CODE_LENGTH + "d", otp);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate TOTP code", e);
        }
    }
    
    /**
     * Generates current TOTP code for testing purposes
     */
    public String generateCurrentCode(String secret) {
        return generateCode(secret, getCurrentTimeWindow());
    }
    
    /**
     * Gets remaining seconds in current time window
     */
    public int getRemainingSeconds() {
        long currentTime = System.currentTimeMillis() / 1000;
        return TIME_STEP - (int) (currentTime % TIME_STEP);
    }
    
    /**
     * Validates secret format
     */
    public boolean isValidSecret(String secret) {
        if (secret == null || secret.trim().isEmpty()) {
            return false;
        }
        
        try {
            Base32 base32 = new Base32();
            byte[] decoded = base32.decode(secret);
            return decoded.length >= 16; // Minimum 128 bits
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Gets TOTP configuration info
     */
    public Map<String, Object> getTOTPConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("algorithm", algorithm.replace("Hmac", ""));
        config.put("digits", CODE_LENGTH);
        config.put("period", TIME_STEP);
        config.put("issuer", issuer);
        return config;
    }
}