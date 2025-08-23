# Enterprise Payment Service

A comprehensive, PCI DSS compliant payment service with multiple provider support, advanced fraud detection, and enterprise-grade security features.

## 🚀 Features

### Core Payment Processing
- **Multiple Payment Providers**: Stripe, PayPal, Square, Bank Transfer, Cryptocurrency
- **Payment Methods**: Credit/Debit Cards, Digital Wallets (Apple Pay, Google Pay), Bank Accounts
- **Payment Flows**: One-time payments, Subscriptions, Installments, Split payments
- **Currency Support**: Multi-currency processing with real-time exchange rates

### Security & Compliance
- **PCI DSS Compliance**: Full PCI DSS Level 1 compliance implementation
- **Encryption**: AES-256-GCM encryption for all sensitive data
- **Tokenization**: Secure tokenization of payment methods
- **3D Secure**: Support for 3D Secure authentication

### Fraud Detection
- **Real-time Analysis**: Advanced fraud detection with machine learning
- **Risk Assessment**: Multi-factor risk scoring
- **Velocity Checking**: Transaction and amount velocity monitoring
- **Geographic Analysis**: IP geolocation and VPN detection
- **Behavioral Analysis**: User behavior pattern recognition

### Financial Operations
- **Refunds & Voids**: Full and partial refund processing
- **Chargeback Management**: Automated chargeback handling
- **Settlement & Reconciliation**: Automated financial reconciliation
- **Fee Management**: Flexible fee calculation and tracking
- **Multi-currency**: Support for 10+ currencies

### Advanced Features
- **Digital Wallets**: Comprehensive wallet management system
- **Escrow Services**: Secure escrow for marketplace transactions
- **Payment Analytics**: Real-time payment analytics and reporting
- **Webhook System**: Reliable webhook delivery with retry logic
- **API Rate Limiting**: Advanced rate limiting and throttling

## 🏗️ Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Client Apps   │────│   Payment API    │────│   Providers     │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │                      │
                        ┌───────┴───────┐             │
                        │               │             │
                   ┌────▼────┐    ┌─────▼─────┐      │
                   │ Fraud   │    │    PCI    │      │
                   │Detection│    │Compliance │      │
                   └─────────┘    └───────────┘      │
                                                     │
┌─────────────────────────────────────────────────────▼─────────────┐
│                     Core Services                                   │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ │
│  │  Payment    │ │   Wallet    │ │   Refund    │ │  Analytics  │ │
│  │  Service    │ │  Service    │ │  Service    │ │   Service   │ │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                │
                     ┌──────────▼──────────┐
                     │     Data Layer      │
                     │  ┌─────────────────┐│
                     │  │    MongoDB      ││
                     │  └─────────────────┘│
                     │  ┌─────────────────┐│
                     │  │     Redis       ││
                     │  └─────────────────┘│
                     └─────────────────────┘
```

## 📋 Requirements

- Node.js >= 16.0.0
- MongoDB >= 4.4
- Redis >= 6.0
- SSL Certificate (for production)

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/payment-service.git
cd payment-service
```

### 2. Install Dependencies

```bash
npm install
```

### 3. Environment Configuration

Copy the example environment file and configure:

```bash
cp .env.example .env
```

Configure your `.env` file:

```env
# Server Configuration
PORT=3000
NODE_ENV=development

# Database
MONGODB_URI=mongodb://localhost:27017/payment-service
REDIS_HOST=localhost
REDIS_PORT=6379

# Security
ENCRYPTION_MASTER_KEY=your-64-char-hex-key
JWT_SECRET=your-jwt-secret
SESSION_SECRET=your-session-secret

# Payment Providers
STRIPE_SECRET_KEY=sk_test_...
STRIPE_PUBLISHABLE_KEY=pk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...

PAYPAL_CLIENT_ID=your-paypal-client-id
PAYPAL_CLIENT_SECRET=your-paypal-client-secret
PAYPAL_MODE=sandbox

# Fraud Detection
ENABLE_FRAUD_DETECTION=true
MAX_VELOCITY_PER_HOUR=5
MAX_VELOCITY_PER_DAY=20
MAX_AMOUNT_PER_HOUR=5000

# PCI Compliance
ENABLE_PCI_COMPLIANCE=true

# CORS
CORS_ORIGIN=http://localhost:3001
```

### 4. Start the Service

```bash
# Development
npm run dev

# Production
npm start
```

The service will be available at `http://localhost:3000`

## 🔧 Configuration

### Payment Providers

#### Stripe Configuration

```javascript
const stripeConfig = {
  secretKey: process.env.STRIPE_SECRET_KEY,
  publishableKey: process.env.STRIPE_PUBLISHABLE_KEY,
  webhookSecret: process.env.STRIPE_WEBHOOK_SECRET,
  timeout: 30000
};
```

#### PayPal Configuration

```javascript
const paypalConfig = {
  clientId: process.env.PAYPAL_CLIENT_ID,
  clientSecret: process.env.PAYPAL_CLIENT_SECRET,
  mode: 'sandbox', // or 'live'
  webhookId: process.env.PAYPAL_WEBHOOK_ID
};
```

### Fraud Detection Rules

```javascript
const fraudConfig = {
  enableFraudDetection: true,
  maxVelocityPerHour: 5,
  maxVelocityPerDay: 20,
  maxAmountPerHour: 5000,
  suspiciousCountries: ['XX', 'YY'],
  enableMLModels: true
};
```

## 📖 API Documentation

### Payment Intents

#### Create Payment Intent

```http
POST /api/v1/payments/intents
Content-Type: application/json

{
  "userId": "user_123",
  "orderId": "order_456",
  "amount": 100.50,
  "currency": "USD",
  "description": "Purchase from Store",
  "provider": "stripe",
  "captureMethod": "automatic",
  "paymentMethodOptions": {
    "card": {
      "requestThreeDSecure": "automatic"
    }
  }
}
```

**Response:**

```json
{
  "intentId": "pi_1234567890",
  "clientSecret": "pi_1234567890_secret_abc123",
  "amount": 100.50,
  "currency": "USD",
  "status": "requires_payment_method",
  "riskAssessment": {
    "score": 15,
    "level": "low"
  }
}
```

#### Confirm Payment Intent

```http
POST /api/v1/payments/intents/:intentId/confirm
Content-Type: application/json

{
  "paymentMethodId": "pm_1234567890",
  "returnUrl": "https://your-site.com/return"
}
```

### Payment Methods

#### Create Payment Method

```http
POST /api/v1/payment-methods
Content-Type: application/json

{
  "userId": "user_123",
  "type": "credit_card",
  "provider": "stripe",
  "card": {
    "number": "4242424242424242",
    "expMonth": 12,
    "expYear": 2025,
    "cvc": "123"
  },
  "billingDetails": {
    "name": "John Doe",
    "email": "john.doe@example.com",
    "address": {
      "line1": "123 Main St",
      "city": "New York",
      "state": "NY",
      "postalCode": "10001",
      "country": "US"
    }
  }
}
```

### Refunds

#### Create Refund

```http
POST /api/v1/payments/refunds
Content-Type: application/json

{
  "paymentId": "pay_1234567890",
  "amount": 50.00,
  "reason": "requested_by_customer",
  "metadata": {
    "order_id": "order_456"
  }
}
```

### Wallets

#### Get Wallet Balance

```http
GET /api/v1/wallets/:userId
```

#### Add Funds to Wallet

```http
POST /api/v1/wallets/:userId/credit
Content-Type: application/json

{
  "amount": 100.00,
  "currency": "USD",
  "description": "Wallet top-up",
  "paymentMethodId": "pm_1234567890"
}
```

## 🔐 Security Features

### PCI DSS Compliance

- **Data Encryption**: All sensitive data encrypted at rest and in transit
- **Access Control**: Role-based access control for all operations
- **Audit Logging**: Comprehensive audit trails for compliance
- **Data Masking**: Automatic masking of sensitive data in logs and responses
- **Secure Storage**: No storage of sensitive authentication data post-authorization

### Fraud Detection

- **Velocity Checking**: Monitor transaction frequency and amounts
- **Geographic Analysis**: IP geolocation and country-based rules
- **Device Fingerprinting**: Track and analyze device characteristics
- **Behavioral Analysis**: User behavior pattern recognition
- **Machine Learning**: AI-powered fraud detection models

### Encryption

```javascript
// All sensitive data is encrypted using AES-256-GCM
const encryptedData = encrypt('4242424242424242', 'card_number');
// Output: "iv:tag:ciphertext" format

// Decryption
const decryptedData = decrypt(encryptedData, 'card_number');
```

## 🧪 Testing

### Running Tests

```bash
# Run all tests
npm test

# Run tests with coverage
npm run test:coverage

# Run tests in watch mode
npm run test:watch
```

### Test Categories

- **Unit Tests**: Individual component testing
- **Integration Tests**: API endpoint testing
- **Security Tests**: PCI compliance and encryption testing
- **Fraud Detection Tests**: Fraud rule engine testing
- **Performance Tests**: Load and stress testing

### Test Data

#### Test Card Numbers

| Card Brand | Number | CVC | Expiry |
|------------|--------|-----|--------|
| Visa | 4242424242424242 | 123 | 12/25 |
| Mastercard | 5555555555554444 | 123 | 12/25 |
| American Express | 378282246310005 | 1234 | 12/25 |
| Declined | 4000000000000002 | 123 | 12/25 |
| Insufficient Funds | 4000000000009995 | 123 | 12/25 |

## 📊 Monitoring & Analytics

### Health Checks

```http
GET /health
```

**Response:**

```json
{
  "status": "healthy",
  "timestamp": "2023-12-01T10:00:00.000Z",
  "service": "payment-service",
  "version": "1.0.0",
  "providers": {
    "stripe": {
      "status": "healthy",
      "responseTime": "45ms"
    },
    "paypal": {
      "status": "healthy",
      "responseTime": "78ms"
    }
  }
}
```

### Metrics

- **Payment Success Rate**: Real-time success rate tracking
- **Average Response Time**: API response time monitoring
- **Fraud Detection Rate**: Fraud detection effectiveness
- **Provider Performance**: Individual provider performance metrics

## 🚀 Deployment

### Docker Deployment

```dockerfile
FROM node:16-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
EXPOSE 3000
CMD ["npm", "start"]
```

```bash
# Build image
docker build -t payment-service .

# Run container
docker run -p 3000:3000 -e NODE_ENV=production payment-service
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payment-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: payment-service
  template:
    metadata:
      labels:
        app: payment-service
    spec:
      containers:
      - name: payment-service
        image: payment-service:latest
        ports:
        - containerPort: 3000
        env:
        - name: NODE_ENV
          value: "production"
        - name: MONGODB_URI
          valueFrom:
            secretKeyRef:
              name: payment-secrets
              key: mongodb-uri
```

### Environment Variables

| Variable | Description | Required | Default |
|----------|-------------|----------|----------|
| PORT | Server port | No | 3000 |
| NODE_ENV | Environment | No | development |
| MONGODB_URI | MongoDB connection string | Yes | - |
| REDIS_HOST | Redis host | No | localhost |
| ENCRYPTION_MASTER_KEY | Master encryption key (64 hex chars) | Yes | - |
| STRIPE_SECRET_KEY | Stripe secret key | No | - |
| PAYPAL_CLIENT_ID | PayPal client ID | No | - |
| ENABLE_FRAUD_DETECTION | Enable fraud detection | No | true |
| ENABLE_PCI_COMPLIANCE | Enable PCI compliance checks | No | true |

## 🔧 Troubleshooting

### Common Issues

#### 1. Database Connection Issues

```bash
# Check MongoDB connection
mongo mongodb://localhost:27017/payment-service

# Check if MongoDB is running
sudo systemctl status mongod
```

#### 2. Encryption Configuration

```javascript
// Generate a new master key
const crypto = require('crypto');
const masterKey = crypto.randomBytes(32).toString('hex');
console.log('ENCRYPTION_MASTER_KEY=' + masterKey);
```

#### 3. Provider Configuration

```bash
# Test Stripe connection
curl -X GET "https://api.stripe.com/v1/payment_methods" \
  -H "Authorization: Bearer sk_test_..."
```

### Error Codes

| Code | Description | Resolution |
|------|-------------|------------|
| PAYMENT_001 | Invalid payment amount | Check amount is positive number |
| FRAUD_001 | High risk transaction | Review fraud detection rules |
| PCI_001 | PCI compliance violation | Review data handling practices |
| PROVIDER_001 | Provider API error | Check provider credentials |
| AUTH_001 | Authentication failed | Verify API keys |

## 📚 API Reference

For complete API documentation, visit:
- Development: `http://localhost:3000/api/docs`
- Swagger/OpenAPI specification available at `/api/swagger.json`

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines

- Follow PCI DSS requirements for all payment-related code
- Write comprehensive tests for new features
- Update documentation for API changes
- Use conventional commit messages
- Ensure all tests pass before submitting PR

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🔗 Related Projects

- [Payment Dashboard](https://github.com/your-org/payment-dashboard) - Admin dashboard for payment management
- [Payment SDK](https://github.com/your-org/payment-sdk) - Client SDK for easy integration
- [Fraud Detection ML](https://github.com/your-org/fraud-detection-ml) - Machine learning models for fraud detection

## 📞 Support

- **Documentation**: [https://docs.your-payment-service.com](https://docs.your-payment-service.com)
- **Issues**: [GitHub Issues](https://github.com/your-org/payment-service/issues)
- **Security**: security@your-org.com
- **General**: support@your-org.com

---

**⚠️ Security Notice**: This is enterprise-grade payment software handling sensitive financial data. Please ensure you comply with all applicable regulations (PCI DSS, GDPR, etc.) and conduct thorough security reviews before production deployment.