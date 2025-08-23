# Data Encryption Implementation Guide

## Overview
This guide provides comprehensive implementation details for data encryption across all states: at rest, in transit, and in use. It covers encryption algorithms, key management, and best practices for securing sensitive data.

## Encryption at Rest

### Database Encryption

#### Transparent Data Encryption (TDE) - PostgreSQL
```sql
-- Enable TDE for PostgreSQL
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Create encrypted tablespace
CREATE TABLESPACE encrypted_space
  LOCATION '/secure/pgdata/encrypted'
  WITH (encryption_key_id = 'arn:aws:kms:us-east-1:123456789012:key/abc-123');

-- Create table with encrypted columns
CREATE TABLE sensitive_data (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    -- Encrypted at column level
    ssn TEXT ENCRYPTED WITH (column_encryption_key = cek1, 
                           encryption_type = 'DETERMINISTIC'),
    credit_card TEXT ENCRYPTED WITH (column_encryption_key = cek2,
                                   encryption_type = 'RANDOMIZED'),
    -- Standard columns
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) TABLESPACE encrypted_space;
```

#### Application-Level Encryption
```java
@Service
public class EncryptionService {
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    
    @Autowired
    private KeyManagementService keyManagementService;
    
    public EncryptedData encrypt(String plaintext, String keyId) {
        try {
            // Get data encryption key
            DataKey dataKey = keyManagementService.generateDataKey(keyId);
            
            // Generate IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            
            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            SecretKeySpec keySpec = new SecretKeySpec(dataKey.getPlaintext(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);
            
            // Encrypt data
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // Return encrypted data with metadata
            return EncryptedData.builder()
                .ciphertext(Base64.getEncoder().encodeToString(ciphertext))
                .iv(Base64.getEncoder().encodeToString(iv))
                .keyId(keyId)
                .encryptedDataKey(dataKey.getCiphertext())
                .algorithm(ALGORITHM)
                .timestamp(Instant.now())
                .build();
                
        } catch (Exception e) {
            throw new EncryptionException("Failed to encrypt data", e);
        }
    }
    
    public String decrypt(EncryptedData encryptedData) {
        try {
            // Decrypt data key
            byte[] decryptedKey = keyManagementService.decryptDataKey(
                encryptedData.getEncryptedDataKey(),
                encryptedData.getKeyId()
            );
            
            // Initialize cipher
            Cipher cipher = Cipher.getInstance(encryptedData.getAlgorithm());
            byte[] iv = Base64.getDecoder().decode(encryptedData.getIv());
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            SecretKeySpec keySpec = new SecretKeySpec(decryptedKey, "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);
            
            // Decrypt data
            byte[] ciphertext = Base64.getDecoder().decode(encryptedData.getCiphertext());
            byte[] plaintext = cipher.doFinal(ciphertext);
            
            return new String(plaintext, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            throw new DecryptionException("Failed to decrypt data", e);
        }
    }
}
```

### File System Encryption

#### Encrypted File Storage Service
```java
@Service
public class EncryptedFileStorageService {
    
    private static final int CHUNK_SIZE = 4 * 1024 * 1024; // 4MB chunks
    private static final String ALGORITHM = "AES/CTR/NoPadding";
    
    @Autowired
    private EncryptionService encryptionService;
    
    @Autowired
    private S3Client s3Client;
    
    public String storeFile(MultipartFile file, String bucketName, String keyId) {
        String fileId = UUID.randomUUID().toString();
        String s3Key = "encrypted-files/" + fileId;
        
        try (InputStream inputStream = file.getInputStream()) {
            // Initialize multipart upload
            CreateMultipartUploadRequest uploadRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .serverSideEncryption(ServerSideEncryption.AWS_KMS)
                .ssekmsKeyId(keyId)
                .metadata(Map.of(
                    "original-filename", file.getOriginalFilename(),
                    "content-type", file.getContentType(),
                    "encrypted-at", Instant.now().toString()
                ))
                .build();
                
            CreateMultipartUploadResponse uploadResponse = s3Client.createMultipartUpload(uploadRequest);
            String uploadId = uploadResponse.uploadId();
            
            List<CompletedPart> completedParts = new ArrayList<>();
            byte[] buffer = new byte[CHUNK_SIZE];
            int partNumber = 1;
            int bytesRead;
            
            // Encrypt and upload chunks
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] chunk = Arrays.copyOf(buffer, bytesRead);
                
                // Encrypt chunk
                EncryptedData encryptedChunk = encryptionService.encrypt(
                    Base64.getEncoder().encodeToString(chunk),
                    keyId
                );
                
                // Upload encrypted chunk
                UploadPartRequest partRequest = UploadPartRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .uploadId(uploadId)
                    .partNumber(partNumber)
                    .build();
                    
                RequestBody requestBody = RequestBody.fromString(
                    encryptedChunk.getCiphertext()
                );
                
                UploadPartResponse partResponse = s3Client.uploadPart(partRequest, requestBody);
                
                completedParts.add(CompletedPart.builder()
                    .partNumber(partNumber)
                    .eTag(partResponse.eTag())
                    .build());
                    
                partNumber++;
            }
            
            // Complete multipart upload
            CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder()
                    .parts(completedParts)
                    .build())
                .build();
                
            s3Client.completeMultipartUpload(completeRequest);
            
            // Store file metadata
            storeFileMetadata(fileId, file, s3Key, keyId);
            
            return fileId;
            
        } catch (Exception e) {
            throw new FileStorageException("Failed to store encrypted file", e);
        }
    }
    
    public void downloadFile(String fileId, OutputStream outputStream) {
        FileMetadata metadata = getFileMetadata(fileId);
        
        GetObjectRequest getRequest = GetObjectRequest.builder()
            .bucket(metadata.getBucketName())
            .key(metadata.getS3Key())
            .build();
            
        try (ResponseInputStream<GetObjectResponse> s3InputStream = 
                s3Client.getObject(getRequest)) {
            
            // Decrypt and stream chunks
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            
            while ((bytesRead = s3InputStream.read(buffer)) != -1) {
                String encryptedChunk = new String(buffer, 0, bytesRead);
                
                // Decrypt chunk
                EncryptedData encryptedData = EncryptedData.builder()
                    .ciphertext(encryptedChunk)
                    .keyId(metadata.getKeyId())
                    .build();
                    
                String decryptedChunk = encryptionService.decrypt(encryptedData);
                byte[] decryptedBytes = Base64.getDecoder().decode(decryptedChunk);
                
                outputStream.write(decryptedBytes);
            }
            
        } catch (Exception e) {
            throw new FileStorageException("Failed to download encrypted file", e);
        }
    }
}
```

## Encryption in Transit

### TLS Configuration

#### Spring Boot TLS Configuration
```yaml
server:
  port: 8443
  ssl:
    enabled: true
    protocol: TLS
    enabled-protocols:
      - TLSv1.2
      - TLSv1.3
    ciphers:
      - TLS_AES_256_GCM_SHA384
      - TLS_CHACHA20_POLY1305_SHA256
      - TLS_AES_128_GCM_SHA256
      - ECDHE-RSA-AES256-GCM-SHA384
      - ECDHE-RSA-AES128-GCM-SHA256
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: server
    trust-store: classpath:truststore.p12
    trust-store-password: ${SSL_TRUSTSTORE_PASSWORD}
    trust-store-type: PKCS12
    client-auth: want

# Enforce HTTPS
security:
  require-ssl: true
  hsts:
    enabled: true
    max-age-seconds: 31536000
    include-subdomains: true
    preload: true
```

#### Custom TLS Configuration
```java
@Configuration
public class TlsConfig {
    
    @Bean
    public EmbeddedServletContainerFactory servletContainer() {
        TomcatEmbeddedServletContainerFactory factory = 
            new TomcatEmbeddedServletContainerFactory();
            
        factory.addConnectorCustomizers(connector -> {
            Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
            
            // Configure TLS
            protocol.setSSLEnabled(true);
            protocol.setSslProtocol("TLS");
            protocol.setSslEnabledProtocols("TLSv1.2,TLSv1.3");
            
            // Set cipher suites
            protocol.setCiphers(String.join(",",
                "TLS_AES_256_GCM_SHA384",
                "TLS_CHACHA20_POLY1305_SHA256",
                "TLS_AES_128_GCM_SHA256"
            ));
            
            // Enable OCSP stapling
            protocol.setSSLStaplingEnabled(true);
            
            // Session settings
            protocol.setSessionCacheSize(50);
            protocol.setSessionTimeout(300);
        });
        
        return factory;
    }
}
```

### Mutual TLS (mTLS) Implementation

#### mTLS Server Configuration
```java
@Configuration
@EnableWebSecurity
public class MutualTlsConfig extends WebSecurityConfigurerAdapter {
    
    @Value("${server.ssl.trust-store}")
    private Resource trustStore;
    
    @Value("${server.ssl.trust-store-password}")
    private String trustStorePassword;
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .anyRequest().authenticated()
            .and()
            .x509()
                .subjectPrincipalRegex("CN=(.*?)(?:,|$)")
                .userDetailsService(x509UserDetailsService());
    }
    
    @Bean
    public UserDetailsService x509UserDetailsService() {
        return username -> {
            // Extract client certificate details
            X509Certificate[] certChain = (X509Certificate[]) 
                SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getCredentials();
                    
            if (certChain == null || certChain.length == 0) {
                throw new UsernameNotFoundException("No client certificate found");
            }
            
            X509Certificate clientCert = certChain[0];
            
            // Validate certificate
            validateCertificate(clientCert);
            
            // Build user details from certificate
            return User.builder()
                .username(username)
                .password("")
                .authorities(extractAuthorities(clientCert))
                .build();
        };
    }
    
    private void validateCertificate(X509Certificate certificate) {
        try {
            // Check certificate validity
            certificate.checkValidity();
            
            // Verify certificate chain
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            CertPath certPath = cf.generateCertPath(Arrays.asList(certificate));
            
            // Create trust anchor from trust store
            KeyStore trustKeyStore = KeyStore.getInstance("PKCS12");
            trustKeyStore.load(trustStore.getInputStream(), 
                             trustStorePassword.toCharArray());
                             
            PKIXParameters params = new PKIXParameters(trustKeyStore);
            params.setRevocationEnabled(true);
            
            CertPathValidator validator = CertPathValidator.getInstance("PKIX");
            validator.validate(certPath, params);
            
        } catch (Exception e) {
            throw new CertificateException("Certificate validation failed", e);
        }
    }
}
```

#### mTLS Client Configuration
```java
@Configuration
public class MutualTlsClientConfig {
    
    @Bean
    public RestTemplate mutualTlsRestTemplate() throws Exception {
        // Load client certificate
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new FileInputStream("client-cert.p12"), 
                     "password".toCharArray());
                     
        SSLContext sslContext = SSLContextBuilder.create()
            .loadKeyMaterial(keyStore, "password".toCharArray())
            .loadTrustMaterial(new File("truststore.jks"), 
                             "trustpass".toCharArray())
            .build();
            
        HostnameVerifier hostnameVerifier = (hostname, session) -> {
            // Implement custom hostname verification if needed
            return HttpsURLConnection.getDefaultHostnameVerifier()
                .verify(hostname, session);
        };
        
        SSLConnectionSocketFactory sslSocketFactory = 
            new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
            
        HttpClient httpClient = HttpClients.custom()
            .setSSLSocketFactory(sslSocketFactory)
            .build();
            
        HttpComponentsClientHttpRequestFactory requestFactory = 
            new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);
        
        return new RestTemplate(requestFactory);
    }
}
```

### Message Queue Encryption

#### Kafka Encryption Configuration
```java
@Configuration
public class KafkaEncryptionConfig {
    
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9093");
        
        // SSL Configuration
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, "/path/to/kafka.client.truststore.jks");
        props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "truststore-password");
        props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, "/path/to/kafka.client.keystore.jks");
        props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "keystore-password");
        props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "key-password");
        
        // Enable encryption at rest
        props.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, 
                 EncryptionProducerInterceptor.class.getName());
        
        return new DefaultKafkaProducerFactory<>(props);
    }
    
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9093");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "encrypted-consumer-group");
        
        // SSL Configuration (same as producer)
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, "/path/to/kafka.client.truststore.jks");
        props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "truststore-password");
        
        // Enable decryption
        props.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG,
                 DecryptionConsumerInterceptor.class.getName());
        
        return new DefaultKafkaConsumerFactory<>(props);
    }
}
```

#### Message Encryption Interceptor
```java
public class EncryptionProducerInterceptor implements ProducerInterceptor<String, Object> {
    
    private EncryptionService encryptionService;
    private String encryptionKeyId;
    
    @Override
    public void configure(Map<String, ?> configs) {
        this.encryptionKeyId = (String) configs.get("encryption.key.id");
        this.encryptionService = SpringContext.getBean(EncryptionService.class);
    }
    
    @Override
    public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> record) {
        try {
            // Serialize value
            String serializedValue = ObjectMapperUtils.serialize(record.value());
            
            // Encrypt
            EncryptedData encryptedData = encryptionService.encrypt(
                serializedValue, 
                encryptionKeyId
            );
            
            // Create new record with encrypted data
            ProducerRecord<String, Object> encryptedRecord = 
                new ProducerRecord<>(
                    record.topic(),
                    record.partition(),
                    record.timestamp(),
                    record.key(),
                    encryptedData,
                    record.headers()
                );
                
            // Add encryption metadata to headers
            encryptedRecord.headers()
                .add("encryption-key-id", encryptionKeyId.getBytes())
                .add("encryption-timestamp", 
                     Instant.now().toString().getBytes());
                     
            return encryptedRecord;
            
        } catch (Exception e) {
            throw new KafkaException("Failed to encrypt message", e);
        }
    }
    
    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        if (exception != null) {
            log.error("Failed to send encrypted message", exception);
        }
    }
    
    @Override
    public void close() {
        // Cleanup resources
    }
}
```

## Encryption in Use

### Field-Level Encryption

#### Format-Preserving Encryption (FPE)
```java
@Service
public class FormatPreservingEncryptionService {
    
    private static final String FF3_CIPHER = "FF3-1-AES256";
    
    @Autowired
    private KeyManagementService keyManagementService;
    
    public String encryptCreditCard(String creditCard) {
        // Remove non-numeric characters
        String digits = creditCard.replaceAll("[^0-9]", "");
        
        if (digits.length() != 16) {
            throw new IllegalArgumentException("Invalid credit card number");
        }
        
        // Preserve first 6 and last 4 digits (BIN and last 4)
        String prefix = digits.substring(0, 6);
        String middle = digits.substring(6, 12);
        String suffix = digits.substring(12, 16);
        
        // Encrypt middle 6 digits using FPE
        String encryptedMiddle = encryptNumeric(middle, 6);
        
        // Reconstruct credit card number
        return formatCreditCard(prefix + encryptedMiddle + suffix);
    }
    
    public String encryptSSN(String ssn) {
        // Remove dashes
        String digits = ssn.replaceAll("-", "");
        
        if (digits.length() != 9) {
            throw new IllegalArgumentException("Invalid SSN");
        }
        
        // Encrypt last 4 digits only
        String prefix = digits.substring(0, 5);
        String suffix = digits.substring(5, 9);
        String encryptedSuffix = encryptNumeric(suffix, 4);
        
        // Format as XXX-XX-YYYY
        return String.format("%s-%s-%s",
            "XXX",
            "XX",
            encryptedSuffix
        );
    }
    
    private String encryptNumeric(String input, int length) {
        try {
            // Get FPE key
            byte[] key = keyManagementService.getFPEKey();
            
            // Create FPE cipher
            FF3Cipher cipher = new FF3Cipher(key, 10); // radix 10 for numeric
            
            // Encrypt
            return cipher.encrypt(input, getTweak());
            
        } catch (Exception e) {
            throw new EncryptionException("FPE encryption failed", e);
        }
    }
    
    private byte[] getTweak() {
        // Generate deterministic tweak based on context
        return "contextTweak".getBytes(StandardCharsets.UTF_8);
    }
    
    private String formatCreditCard(String digits) {
        return String.format("%s-%s-%s-%s",
            digits.substring(0, 4),
            digits.substring(4, 8),
            digits.substring(8, 12),
            digits.substring(12, 16)
        );
    }
}
```

#### Searchable Encryption
```java
@Service
public class SearchableEncryptionService {
    
    @Autowired
    private EncryptionService encryptionService;
    
    @Autowired
    private HashingService hashingService;
    
    public EncryptedSearchableData encryptSearchable(String plaintext, 
                                                     String keyId,
                                                     Set<String> searchableFields) {
        // Encrypt full data
        EncryptedData encryptedData = encryptionService.encrypt(plaintext, keyId);
        
        // Generate search tokens
        Map<String, String> searchTokens = new HashMap<>();
        for (String field : searchableFields) {
            String fieldValue = extractFieldValue(plaintext, field);
            String token = generateSearchToken(fieldValue, keyId);
            searchTokens.put(field, token);
        }
        
        return EncryptedSearchableData.builder()
            .encryptedData(encryptedData)
            .searchTokens(searchTokens)
            .build();
    }
    
    public List<String> searchEncrypted(String searchTerm, 
                                       String field,
                                       String keyId) {
        // Generate search token for the term
        String searchToken = generateSearchToken(searchTerm, keyId);
        
        // Query database using the token
        return repository.findBySearchToken(field, searchToken);
    }
    
    private String generateSearchToken(String value, String keyId) {
        // Use HMAC to generate deterministic token
        String salt = keyId + ":search";
        return hashingService.hmac(value.toLowerCase(), salt);
    }
    
    private String extractFieldValue(String data, String field) {
        // Parse JSON and extract field value
        try {
            JsonNode node = ObjectMapperUtils.readTree(data);
            return node.get(field).asText();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to extract field: " + field);
        }
    }
}
```

### Secure Enclaves

#### Intel SGX Implementation
```cpp
// Enclave code for sensitive operations
#include "sgx_tcrypto.h"
#include "sgx_trts.h"

// Sealed data structure
typedef struct _sealed_data_t {
    uint32_t data_size;
    uint8_t encrypted_data[0];
} sealed_data_t;

// Encrypt sensitive data within enclave
sgx_status_t enclave_encrypt_data(
    const uint8_t* plaintext,
    uint32_t plaintext_size,
    uint8_t* ciphertext,
    uint32_t* ciphertext_size) {
    
    sgx_status_t status = SGX_SUCCESS;
    sgx_aes_gcm_128bit_key_t key;
    uint8_t iv[SGX_AESGCM_IV_SIZE] = {0};
    
    // Generate random key within enclave
    status = sgx_read_rand((uint8_t*)&key, sizeof(key));
    if (status != SGX_SUCCESS) return status;
    
    // Generate random IV
    status = sgx_read_rand(iv, SGX_AESGCM_IV_SIZE);
    if (status != SGX_SUCCESS) return status;
    
    // Encrypt data
    sgx_aes_gcm_128bit_tag_t mac;
    status = sgx_rijndael128GCM_encrypt(
        &key,
        plaintext,
        plaintext_size,
        ciphertext,
        iv,
        SGX_AESGCM_IV_SIZE,
        NULL,
        0,
        &mac
    );
    
    if (status == SGX_SUCCESS) {
        *ciphertext_size = plaintext_size;
        
        // Seal the key for persistent storage
        seal_key(&key);
    }
    
    return status;
}

// Seal sensitive data for persistent storage
sgx_status_t seal_sensitive_data(
    const uint8_t* data,
    uint32_t data_size,
    sgx_sealed_data_t* sealed_data,
    uint32_t sealed_size) {
    
    sgx_status_t status = SGX_SUCCESS;
    
    // Check sealed data size
    uint32_t encrypt_data_size = sgx_calc_sealed_data_size(0, data_size);
    if (encrypt_data_size == UINT32_MAX) {
        return SGX_ERROR_UNEXPECTED;
    }
    
    if (encrypt_data_size > sealed_size) {
        return SGX_ERROR_INVALID_PARAMETER;
    }
    
    // Seal data with MRSIGNER policy
    status = sgx_seal_data(
        0,                    // No additional MAC data
        NULL,                 // No additional MAC data
        data_size,           // Data to encrypt size
        data,                // Data to encrypt
        encrypt_data_size,   // Sealed data size
        sealed_data          // Output
    );
    
    return status;
}
```

#### AWS Nitro Enclaves
```python
import boto3
import base64
import json
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.backends import default_backend

class NitroEnclaveService:
    def __init__(self):
        self.kms_client = boto3.client('kms')
        self.enclave_client = boto3.client('ec2')
        
    def process_sensitive_data(self, enclave_id, sensitive_data):
        """Process sensitive data within Nitro Enclave"""
        
        # Prepare attestation document
        attestation_doc = self._get_attestation_document(enclave_id)
        
        # Request data key from KMS with attestation
        response = self.kms_client.decrypt(
            CiphertextBlob=sensitive_data['encrypted_key'],
            EncryptionContext={
                'aws:enclave:attestation': base64.b64encode(
                    attestation_doc
                ).decode('utf-8')
            }
        )
        
        # Decrypt data within enclave
        plaintext_key = response['Plaintext']
        decrypted_data = self._decrypt_in_enclave(
            sensitive_data['ciphertext'],
            plaintext_key,
            sensitive_data['iv']
        )
        
        # Process data (example: PII detection)
        processed_data = self._process_pii(decrypted_data)
        
        # Re-encrypt processed data
        encrypted_result = self._encrypt_in_enclave(
            processed_data,
            plaintext_key
        )
        
        return encrypted_result
    
    def _decrypt_in_enclave(self, ciphertext, key, iv):
        """Decrypt data within the secure enclave"""
        cipher = Cipher(
            algorithms.AES(key),
            modes.GCM(base64.b64decode(iv)),
            backend=default_backend()
        )
        decryptor = cipher.decryptor()
        
        plaintext = decryptor.update(base64.b64decode(ciphertext))
        plaintext += decryptor.finalize()
        
        return plaintext.decode('utf-8')
    
    def _process_pii(self, data):
        """Process PII data within enclave"""
        # This runs in the secure enclave environment
        import re
        
        # Mask sensitive patterns
        data_dict = json.loads(data)
        
        # SSN pattern
        if 'ssn' in data_dict:
            data_dict['ssn'] = re.sub(
                r'(\d{3})-(\d{2})-(\d{4})',
                r'XXX-XX-\3',
                data_dict['ssn']
            )
        
        # Credit card pattern
        if 'credit_card' in data_dict:
            data_dict['credit_card'] = re.sub(
                r'(\d{4})-(\d{4})-(\d{4})-(\d{4})',
                r'XXXX-XXXX-XXXX-\4',
                data_dict['credit_card']
            )
        
        return json.dumps(data_dict)
    
    def _get_attestation_document(self, enclave_id):
        """Get attestation document from Nitro Enclave"""
        # This would be called from within the enclave
        # Returns cryptographically signed attestation
        pass
```

## Key Management Implementation

### AWS KMS Integration
```java
@Service
public class AWSKeyManagementService implements KeyManagementService {
    
    private final AWSKMSClient kmsClient;
    private final String customerMasterKeyId;
    
    @Autowired
    public AWSKeyManagementService(
            @Value("${aws.kms.key-id}") String keyId,
            @Value("${aws.region}") String region) {
        
        this.customerMasterKeyId = keyId;
        this.kmsClient = AWSKMSClientBuilder.standard()
            .withRegion(region)
            .build();
    }
    
    @Override
    public DataKey generateDataKey(String keyId) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest()
            .withKeyId(keyId != null ? keyId : customerMasterKeyId)
            .withKeySpec(DataKeySpec.AES_256);
            
        GenerateDataKeyResult result = kmsClient.generateDataKey(request);
        
        return DataKey.builder()
            .plaintext(result.getPlaintext().array())
            .ciphertext(Base64.getEncoder().encodeToString(
                result.getCiphertextBlob().array()
            ))
            .keyId(result.getKeyId())
            .build();
    }
    
    @Override
    public byte[] decryptDataKey(String encryptedKey, String keyId) {
        DecryptRequest request = new DecryptRequest()
            .withCiphertextBlob(ByteBuffer.wrap(
                Base64.getDecoder().decode(encryptedKey)
            ))
            .withKeyId(keyId);
            
        DecryptResult result = kmsClient.decrypt(request);
        return result.getPlaintext().array();
    }
    
    @Override
    @Cacheable(value = "dataKeys", key = "#keyId")
    public byte[] getOrCreateDataKey(String keyId, Duration ttl) {
        // Check cache first (handled by @Cacheable)
        
        // Generate new data key
        DataKey dataKey = generateDataKey(keyId);
        
        // Store encrypted key for later use
        storeEncryptedKey(keyId, dataKey.getCiphertext());
        
        return dataKey.getPlaintext();
    }
    
    @Scheduled(fixedDelay = 86400000) // Daily
    public void rotateDataKeys() {
        List<String> activeKeyIds = getActiveKeyIds();
        
        for (String keyId : activeKeyIds) {
            try {
                // Create new version
                CreateAliasRequest request = new CreateAliasRequest()
                    .withAliasName("alias/" + keyId + "-v" + getNextVersion(keyId))
                    .withTargetKeyId(customerMasterKeyId);
                    
                kmsClient.createAlias(request);
                
                // Schedule old version for deletion
                scheduleKeyDeletion(keyId, 30); // 30 days
                
                log.info("Rotated key: {}", keyId);
                
            } catch (Exception e) {
                log.error("Failed to rotate key: {}", keyId, e);
            }
        }
    }
}
```

### HashiCorp Vault Integration
```java
@Service
public class VaultKeyManagementService {
    
    private final VaultTemplate vaultTemplate;
    
    @Autowired
    public VaultKeyManagementService(VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
    }
    
    public String generateTransitKey(String keyName) {
        vaultTemplate.opsForTransit().createKey(keyName,
            VaultTransitKeyCreationRequest.builder()
                .type("aes256-gcm96")
                .derived(false)
                .exportable(false)
                .allowPlaintextBackup(false)
                .build()
        );
        
        return keyName;
    }
    
    public String encryptWithTransit(String keyName, String plaintext) {
        VaultEncryptionResult result = vaultTemplate.opsForTransit()
            .encrypt(keyName, plaintext);
            
        return result.getCiphertext();
    }
    
    public String decryptWithTransit(String keyName, String ciphertext) {
        VaultDecryptionResult result = vaultTemplate.opsForTransit()
            .decrypt(keyName, ciphertext);
            
        return result.getPlaintext();
    }
    
    public void configureDynamicSecrets() {
        // Database credentials
        vaultTemplate.opsForSys().mount("database", 
            VaultMount.builder()
                .type("database")
                .description("Dynamic database credentials")
                .build()
        );
        
        Map<String, Object> config = new HashMap<>();
        config.put("plugin_name", "postgresql-database-plugin");
        config.put("allowed_roles", Arrays.asList("readonly", "readwrite"));
        config.put("connection_url", 
            "postgresql://{{username}}:{{password}}@localhost:5432/mydb");
        config.put("username", "vault");
        config.put("password", vaultTemplate.opsForSys()
            .read("secret/database/postgres/admin")
            .getData()
            .get("password"));
            
        vaultTemplate.write("database/config/postgresql", config);
        
        // Configure role
        Map<String, Object> roleConfig = new HashMap<>();
        roleConfig.put("db_name", "postgresql");
        roleConfig.put("creation_statements", Arrays.asList(
            "CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' " +
            "VALID UNTIL '{{expiration}}';",
            "GRANT SELECT ON ALL TABLES IN SCHEMA public TO \"{{name}}\";"
        ));
        roleConfig.put("default_ttl", "1h");
        roleConfig.put("max_ttl", "24h");
        
        vaultTemplate.write("database/roles/readonly", roleConfig);
    }
    
    public DatabaseCredentials getDynamicDatabaseCredentials(String role) {
        VaultResponse response = vaultTemplate.read(
            "database/creds/" + role
        );
        
        return DatabaseCredentials.builder()
            .username(response.getData().get("username").toString())
            .password(response.getData().get("password").toString())
            .leaseId(response.getLeaseId())
            .leaseDuration(response.getLeaseDuration())
            .build();
    }
}
```

## Encryption Monitoring and Compliance

### Encryption Metrics Service
```java
@Service
public class EncryptionMetricsService {
    
    private final MeterRegistry meterRegistry;
    private final Counter encryptionCounter;
    private final Counter decryptionCounter;
    private final Timer encryptionTimer;
    private final Timer decryptionTimer;
    
    public EncryptionMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.encryptionCounter = Counter.builder("encryption.operations")
            .description("Total encryption operations")
            .tag("operation", "encrypt")
            .register(meterRegistry);
            
        this.decryptionCounter = Counter.builder("encryption.operations")
            .description("Total decryption operations")
            .tag("operation", "decrypt")
            .register(meterRegistry);
            
        this.encryptionTimer = Timer.builder("encryption.duration")
            .description("Encryption operation duration")
            .tag("operation", "encrypt")
            .register(meterRegistry);
            
        this.decryptionTimer = Timer.builder("encryption.duration")
            .description("Decryption operation duration")
            .tag("operation", "decrypt")
            .register(meterRegistry);
    }
    
    public void recordEncryption(String algorithm, int dataSize, Duration duration) {
        encryptionCounter.increment();
        encryptionTimer.record(duration);
        
        meterRegistry.gauge("encryption.data.size",
            Tags.of("algorithm", algorithm, "operation", "encrypt"),
            dataSize
        );
    }
    
    public void recordDecryption(String algorithm, int dataSize, Duration duration) {
        decryptionCounter.increment();
        decryptionTimer.record(duration);
        
        meterRegistry.gauge("encryption.data.size",
            Tags.of("algorithm", algorithm, "operation", "decrypt"),
            dataSize
        );
    }
    
    public void recordKeyRotation(String keyId, boolean success) {
        meterRegistry.counter("encryption.key.rotation",
            Tags.of("key_id", keyId, "status", success ? "success" : "failure")
        ).increment();
    }
    
    public EncryptionHealthStatus getHealthStatus() {
        double encryptionRate = encryptionCounter.count() / 
            (System.currentTimeMillis() / 1000.0);
        double decryptionRate = decryptionCounter.count() / 
            (System.currentTimeMillis() / 1000.0);
            
        return EncryptionHealthStatus.builder()
            .totalEncryptions(encryptionCounter.count())
            .totalDecryptions(decryptionCounter.count())
            .encryptionRate(encryptionRate)
            .decryptionRate(decryptionRate)
            .averageEncryptionTime(encryptionTimer.mean(TimeUnit.MILLISECONDS))
            .averageDecryptionTime(decryptionTimer.mean(TimeUnit.MILLISECONDS))
            .build();
    }
}
```

### Encryption Compliance Validator
```java
@Component
public class EncryptionComplianceValidator {
    
    @Autowired
    private EncryptionPolicyService policyService;
    
    public ComplianceReport validateCompliance(String dataClassification) {
        ComplianceReport report = new ComplianceReport();
        EncryptionPolicy policy = policyService.getPolicyForClassification(
            dataClassification
        );
        
        // Validate encryption algorithms
        if (!isApprovedAlgorithm(policy.getRequiredAlgorithm())) {
            report.addViolation("Unapproved encryption algorithm");
        }
        
        // Validate key length
        if (policy.getMinimumKeyLength() > getCurrentKeyLength()) {
            report.addViolation("Insufficient key length");
        }
        
        // Validate key rotation
        if (isKeyRotationOverdue(policy.getKeyRotationPeriod())) {
            report.addViolation("Overdue key rotation");
        }
        
        // Validate data retention
        if (hasExpiredData(policy.getDataRetentionPeriod())) {
            report.addViolation("Expired data not purged");
        }
        
        return report;
    }
    
    private boolean isApprovedAlgorithm(String algorithm) {
        List<String> approvedAlgorithms = Arrays.asList(
            "AES-256-GCM",
            "AES-256-CTR",
            "ChaCha20-Poly1305"
        );
        
        return approvedAlgorithms.contains(algorithm);
    }
}
```

## Conclusion

This comprehensive encryption implementation guide ensures data protection across all states. Regular security audits and compliance checks should be performed to maintain the effectiveness of these encryption measures.

### Implementation Checklist
- [ ] Database encryption (TDE) configured
- [ ] Application-level encryption implemented
- [ ] File system encryption deployed
- [ ] TLS 1.3 configured for all endpoints
- [ ] mTLS implemented for service-to-service communication
- [ ] Message queue encryption enabled
- [ ] Field-level encryption for PII data
- [ ] Secure enclaves configured for sensitive operations
- [ ] Key management system integrated
- [ ] Encryption metrics and monitoring in place
- [ ] Compliance validation automated
- [ ] Key rotation policies implemented
- [ ] Encryption performance benchmarks met