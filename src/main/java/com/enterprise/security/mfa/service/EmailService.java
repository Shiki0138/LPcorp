package com.enterprise.security.mfa.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.internet.MimeMessage;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final int CODE_LENGTH = 6;
    private static final int CODE_VALIDITY_MINUTES = 10;
    private static final int MAX_ATTEMPTS_PER_EMAIL = 5;
    private static final int MAX_ATTEMPTS_PER_IP = 10;
    private static final int RATE_LIMIT_WINDOW_MINUTES = 60;
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired(required = false)
    private TemplateEngine templateEngine;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.name:Enterprise App}")
    private String appName;
    
    @Value("${app.mfa.email.subject-template:Verification Code for %s}")
    private String subjectTemplate;
    
    @Value("${app.mfa.email.plain-template:Your verification code is: %s. This code will expire in %d minutes.}")
    private String plainTextTemplate;
    
    @Value("${app.mfa.email.use-html:true}")
    private boolean useHtmlTemplate;
    
    private final SecureRandom secureRandom = new SecureRandom();
    private final Pattern emailPattern = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    // Rate limiting maps
    private final Map<String, AttemptTracker> emailAttempts = new ConcurrentHashMap<>();
    private final Map<String, AttemptTracker> ipAttempts = new ConcurrentHashMap<>();
    
    /**
     * Sends email verification code
     */
    public boolean sendVerificationCode(String email, String code, String ipAddress) {
        return sendVerificationCode(email, code, ipAddress, null);
    }
    
    /**
     * Sends email verification code with additional context
     */
    public boolean sendVerificationCode(String email, String code, String ipAddress, Map<String, Object> context) {
        // Check rate limits
        if (!checkRateLimit(email, ipAddress)) {
            logger.warn("Rate limit exceeded for email: {} or IP: {}", maskEmail(email), ipAddress);
            return false;
        }
        
        try {
            boolean sent = false;
            
            if (useHtmlTemplate && templateEngine != null) {
                sent = sendHtmlEmail(email, code, context);
            } else {
                sent = sendPlainTextEmail(email, code);
            }
            
            if (sent) {
                // Update rate limiting counters
                incrementAttempts(email, ipAddress);
                logger.info("Verification email sent successfully to: {}", maskEmail(email));
            } else {
                logger.error("Failed to send verification email to: {}", maskEmail(email));
            }
            
            return sent;
            
        } catch (Exception e) {
            logger.error("Error sending verification email to: {} - {}", maskEmail(email), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Generates alphanumeric verification code
     */
    public String generateCode() {
        int code = secureRandom.nextInt((int) Math.pow(10, CODE_LENGTH));
        return String.format("%0" + CODE_LENGTH + "d", code);
    }
    
    /**
     * Generates secure alphanumeric code
     */
    public String generateAlphanumericCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            int index = secureRandom.nextInt(chars.length());
            code.append(chars.charAt(index));
        }
        
        return code.toString();
    }
    
    /**
     * Validates email format
     */
    public boolean isValidEmail(String email) {
        return email != null && emailPattern.matcher(email.trim()).matches();
    }
    
    /**
     * Normalizes email format
     */
    public String normalizeEmail(String email) {
        return email != null ? email.trim().toLowerCase() : null;
    }
    
    // Private methods for sending emails
    
    private boolean sendPlainTextEmail(String email, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject(String.format(subjectTemplate, appName));
            message.setText(String.format(plainTextTemplate, code, CODE_VALIDITY_MINUTES));
            
            mailSender.send(message);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send plain text email: {}", e.getMessage(), e);
            return false;
        }
    }
    
    private boolean sendHtmlEmail(String email, String code, Map<String, Object> context) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject(String.format(subjectTemplate, appName));
            
            // Create template context
            Context templateContext = new Context();
            templateContext.setVariable("code", code);
            templateContext.setVariable("appName", appName);
            templateContext.setVariable("validityMinutes", CODE_VALIDITY_MINUTES);
            templateContext.setVariable("year", LocalDateTime.now().getYear());
            
            // Add additional context if provided
            if (context != null) {
                context.forEach(templateContext::setVariable);
            }
            
            // Process HTML template
            String htmlContent = templateEngine.process("mfa/email-verification", templateContext);
            helper.setText(htmlContent, true);
            
            // Add plain text fallback
            String plainText = String.format(plainTextTemplate, code, CODE_VALIDITY_MINUTES);
            helper.setText(plainText, htmlContent);
            
            mailSender.send(mimeMessage);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send HTML email: {}", e.getMessage(), e);
            // Fallback to plain text
            return sendPlainTextEmail(email, code);
        }
    }
    
    // Rate limiting methods
    
    private boolean checkRateLimit(String email, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        
        // Check email rate limit
        AttemptTracker emailTracker = emailAttempts.get(email);
        if (emailTracker != null && !emailTracker.canAttempt(now, MAX_ATTEMPTS_PER_EMAIL)) {
            return false;
        }
        
        // Check IP address rate limit
        AttemptTracker ipTracker = ipAttempts.get(ipAddress);
        if (ipTracker != null && !ipTracker.canAttempt(now, MAX_ATTEMPTS_PER_IP)) {
            return false;
        }
        
        return true;
    }
    
    private void incrementAttempts(String email, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        
        emailAttempts.computeIfAbsent(email, k -> new AttemptTracker())
            .incrementAttempt(now);
        
        ipAttempts.computeIfAbsent(ipAddress, k -> new AttemptTracker())
            .incrementAttempt(now);
    }
    
    /**
     * Gets remaining attempts for email
     */
    public int getRemainingAttempts(String email) {
        AttemptTracker tracker = emailAttempts.get(email);
        if (tracker == null) {
            return MAX_ATTEMPTS_PER_EMAIL;
        }
        
        return Math.max(0, MAX_ATTEMPTS_PER_EMAIL - tracker.getAttemptCount(LocalDateTime.now()));
    }
    
    /**
     * Resets rate limiting for email (admin function)
     */
    public void resetRateLimit(String email) {
        emailAttempts.remove(email);
        logger.info("Rate limit reset for email: {}", maskEmail(email));
    }
    
    /**
     * Cleans up expired rate limiting entries
     */
    public void cleanupExpiredEntries() {
        LocalDateTime cutoff = LocalDateTime.now().minus(RATE_LIMIT_WINDOW_MINUTES, ChronoUnit.MINUTES);
        
        emailAttempts.entrySet().removeIf(entry -> 
            entry.getValue().getLastAttempt().isBefore(cutoff));
        
        ipAttempts.entrySet().removeIf(entry -> 
            entry.getValue().getLastAttempt().isBefore(cutoff));
    }
    
    /**
     * Sends notification email for security events
     */
    public boolean sendSecurityNotification(String email, String eventType, Map<String, Object> details) {
        try {
            String subject = String.format("Security Alert - %s", appName);
            
            if (useHtmlTemplate && templateEngine != null) {
                return sendSecurityNotificationHtml(email, subject, eventType, details);
            } else {
                return sendSecurityNotificationPlain(email, subject, eventType, details);
            }
            
        } catch (Exception e) {
            logger.error("Failed to send security notification to: {} - {}", maskEmail(email), e.getMessage(), e);
            return false;
        }
    }
    
    private boolean sendSecurityNotificationPlain(String email, String subject, String eventType, Map<String, Object> details) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject(subject);
            
            StringBuilder text = new StringBuilder();
            text.append("A security event has occurred on your account:\n\n");
            text.append("Event: ").append(eventType).append("\n");
            text.append("Time: ").append(LocalDateTime.now()).append("\n");
            
            if (details != null) {
                details.forEach((key, value) -> 
                    text.append(key).append(": ").append(value).append("\n"));
            }
            
            text.append("\nIf this was not you, please contact support immediately.");
            
            message.setText(text.toString());
            mailSender.send(message);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send plain text security notification: {}", e.getMessage(), e);
            return false;
        }
    }
    
    private boolean sendSecurityNotificationHtml(String email, String subject, String eventType, Map<String, Object> details) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject(subject);
            
            Context context = new Context();
            context.setVariable("appName", appName);
            context.setVariable("eventType", eventType);
            context.setVariable("timestamp", LocalDateTime.now());
            context.setVariable("details", details);
            context.setVariable("year", LocalDateTime.now().getYear());
            
            String htmlContent = templateEngine.process("mfa/security-notification", context);
            helper.setText(htmlContent, true);
            
            mailSender.send(mimeMessage);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send HTML security notification: {}", e.getMessage(), e);
            return sendSecurityNotificationPlain(email, subject, eventType, details);
        }
    }
    
    // Utility methods
    
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "****";
        }
        
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 3) {
            return "***@" + domain;
        }
        
        return localPart.substring(0, 2) + "***@" + domain;
    }
    
    /**
     * Inner class for tracking attempt counts and timestamps
     */
    private static class AttemptTracker {
        private final AtomicInteger count = new AtomicInteger(0);
        private LocalDateTime windowStart = LocalDateTime.now();
        private LocalDateTime lastAttempt = LocalDateTime.now();
        
        public synchronized boolean canAttempt(LocalDateTime now, int maxAttempts) {
            // Reset window if enough time has passed
            if (now.isAfter(windowStart.plus(RATE_LIMIT_WINDOW_MINUTES, ChronoUnit.MINUTES))) {
                count.set(0);
                windowStart = now;
            }
            
            return count.get() < maxAttempts;
        }
        
        public synchronized void incrementAttempt(LocalDateTime now) {
            // Reset window if enough time has passed
            if (now.isAfter(windowStart.plus(RATE_LIMIT_WINDOW_MINUTES, ChronoUnit.MINUTES))) {
                count.set(0);
                windowStart = now;
            }
            
            count.incrementAndGet();
            lastAttempt = now;
        }
        
        public synchronized int getAttemptCount(LocalDateTime now) {
            // Reset window if enough time has passed
            if (now.isAfter(windowStart.plus(RATE_LIMIT_WINDOW_MINUTES, ChronoUnit.MINUTES))) {
                count.set(0);
                windowStart = now;
            }
            
            return count.get();
        }
        
        public LocalDateTime getLastAttempt() {
            return lastAttempt;
        }
    }
}