package com.hive.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.util.Properties;

/**
 * Configuration class for notification service
 */
@Configuration
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class NotificationConfiguration {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.mail")
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");
        
        return mailSender;
    }
}

/**
 * Configuration properties for notification settings
 */
@ConfigurationProperties(prefix = "notification")
public class NotificationProperties {
    
    private Email email = new Email();
    private Sms sms = new Sms();
    private Push push = new Push();
    private Queue queue = new Queue();
    private Analytics analytics = new Analytics();

    // Email configuration
    public static class Email {
        private String fromAddress = "noreply@hive.com";
        private String fromName = "Hive Notifications";
        private boolean trackOpens = true;
        private boolean trackClicks = true;
        private int maxRetries = 3;

        // Getters and setters
        public String getFromAddress() { return fromAddress; }
        public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }

        public String getFromName() { return fromName; }
        public void setFromName(String fromName) { this.fromName = fromName; }

        public boolean isTrackOpens() { return trackOpens; }
        public void setTrackOpens(boolean trackOpens) { this.trackOpens = trackOpens; }

        public boolean isTrackClicks() { return trackClicks; }
        public void setTrackClicks(boolean trackClicks) { this.trackClicks = trackClicks; }

        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    }

    // SMS configuration
    public static class Sms {
        private String provider = "twilio";
        private Twilio twilio = new Twilio();
        private Aws aws = new Aws();

        public static class Twilio {
            private String accountSid;
            private String authToken;
            private String fromNumber;

            // Getters and setters
            public String getAccountSid() { return accountSid; }
            public void setAccountSid(String accountSid) { this.accountSid = accountSid; }

            public String getAuthToken() { return authToken; }
            public void setAuthToken(String authToken) { this.authToken = authToken; }

            public String getFromNumber() { return fromNumber; }
            public void setFromNumber(String fromNumber) { this.fromNumber = fromNumber; }
        }

        public static class Aws {
            private String accessKey;
            private String secretKey;
            private String region = "us-east-1";

            // Getters and setters
            public String getAccessKey() { return accessKey; }
            public void setAccessKey(String accessKey) { this.accessKey = accessKey; }

            public String getSecretKey() { return secretKey; }
            public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

            public String getRegion() { return region; }
            public void setRegion(String region) { this.region = region; }
        }

        // Getters and setters
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public Twilio getTwilio() { return twilio; }
        public void setTwilio(Twilio twilio) { this.twilio = twilio; }

        public Aws getAws() { return aws; }
        public void setAws(Aws aws) { this.aws = aws; }
    }

    // Push notification configuration
    public static class Push {
        private Firebase firebase = new Firebase();
        private Apns apns = new Apns();

        public static class Firebase {
            private String serverKey;
            private String url = "https://fcm.googleapis.com/fcm/send";

            // Getters and setters
            public String getServerKey() { return serverKey; }
            public void setServerKey(String serverKey) { this.serverKey = serverKey; }

            public String getUrl() { return url; }
            public void setUrl(String url) { this.url = url; }
        }

        public static class Apns {
            private String keyId;
            private String teamId;
            private String bundleId;
            private String keyPath;

            // Getters and setters
            public String getKeyId() { return keyId; }
            public void setKeyId(String keyId) { this.keyId = keyId; }

            public String getTeamId() { return teamId; }
            public void setTeamId(String teamId) { this.teamId = teamId; }

            public String getBundleId() { return bundleId; }
            public void setBundleId(String bundleId) { this.bundleId = bundleId; }

            public String getKeyPath() { return keyPath; }
            public void setKeyPath(String keyPath) { this.keyPath = keyPath; }
        }

        // Getters and setters
        public Firebase getFirebase() { return firebase; }
        public void setFirebase(Firebase firebase) { this.firebase = firebase; }

        public Apns getApns() { return apns; }
        public void setApns(Apns apns) { this.apns = apns; }
    }

    // Queue configuration
    public static class Queue {
        private int batchSize = 100;
        private int maxRetries = 3;
        private int processingThreads = 10;
        private boolean enablePriorityQueue = true;

        // Getters and setters
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

        public int getProcessingThreads() { return processingThreads; }
        public void setProcessingThreads(int processingThreads) { this.processingThreads = processingThreads; }

        public boolean isEnablePriorityQueue() { return enablePriorityQueue; }
        public void setEnablePriorityQueue(boolean enablePriorityQueue) { this.enablePriorityQueue = enablePriorityQueue; }
    }

    // Analytics configuration
    public static class Analytics {
        private boolean enabled = true;
        private int retentionDays = 90;
        private boolean realTimeMetrics = true;

        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getRetentionDays() { return retentionDays; }
        public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }

        public boolean isRealTimeMetrics() { return realTimeMetrics; }
        public void setRealTimeMetrics(boolean realTimeMetrics) { this.realTimeMetrics = realTimeMetrics; }
    }

    // Main getters and setters
    public Email getEmail() { return email; }
    public void setEmail(Email email) { this.email = email; }

    public Sms getSms() { return sms; }
    public void setSms(Sms sms) { this.sms = sms; }

    public Push getPush() { return push; }
    public void setPush(Push push) { this.push = push; }

    public Queue getQueue() { return queue; }
    public void setQueue(Queue queue) { this.queue = queue; }

    public Analytics getAnalytics() { return analytics; }
    public void setAnalytics(Analytics analytics) { this.analytics = analytics; }
}