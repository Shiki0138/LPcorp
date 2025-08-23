package com.enterprise.security.mfa.config;

import com.enterprise.security.mfa.service.*;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
@EnableScheduling
public class MfaConfiguration {
    
    @Value("${app.mfa.webauthn.rp-id:localhost}")
    private String rpId;
    
    @Value("${app.mfa.webauthn.rp-name:Enterprise App}")
    private String rpName;
    
    @Value("${spring.mail.host:smtp.gmail.com}")
    private String mailHost;
    
    @Value("${spring.mail.port:587}")
    private int mailPort;
    
    @Value("${spring.mail.username:}")
    private String mailUsername;
    
    @Value("${spring.mail.password:}")
    private String mailPassword;
    
    /**
     * Password encoder for backup codes and other hashing needs
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    
    /**
     * JavaMailSender configuration for email MFA
     */
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);
        mailSender.setUsername(mailUsername);
        mailSender.setPassword(mailPassword);
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");
        
        return mailSender;
    }
    
    /**
     * WebAuthn RelyingParty configuration
     */
    @Bean
    public RelyingParty relyingParty() {
        return RelyingParty.builder()
            .identity(RelyingPartyIdentity.builder()
                .id(rpId)
                .name(rpName)
                .build())
            .allowUntrustedAttestation(false)
            .allowOriginPort(true) // For development - disable in production
            .build();
    }
}