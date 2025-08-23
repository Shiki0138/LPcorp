# Client Management System - Installation Guide

## Prerequisites

- Node.js 18+ 
- PostgreSQL 15+
- Redis 7+
- Docker (optional)

## Quick Start with Docker

1. **Clone and setup**:
   ```bash
   cd app
   cp .env.example .env.local
   ```

2. **Update environment variables** in `.env.local`:
   ```bash
   DATABASE_URL="postgresql://postgres:postgres@localhost:5432/client_management_db"
   REDIS_URL="redis://localhost:6379"
   NEXTAUTH_SECRET="your-secret-key-here"
   NEXTAUTH_URL="http://localhost:3000"
   
   # Email Configuration
   EMAIL_FROM="noreply@yourcompany.com"
   EMAIL_HOST="smtp.gmail.com" 
   EMAIL_PORT="587"
   EMAIL_USER="your-email@gmail.com"
   EMAIL_PASS="your-app-password"
   ```

3. **Start services**:
   ```bash
   docker-compose up -d
   ```

4. **Setup database**:
   ```bash
   npm run db:migrate
   npm run db:seed
   ```

5. **Access the application**:
   - Main site: http://localhost:3000
   - Admin login: admin@company.com / admin123

## Manual Installation

### 1. Database Setup

**PostgreSQL:**
```bash
# Install PostgreSQL
brew install postgresql  # macOS
# or
sudo apt-get install postgresql postgresql-contrib  # Ubuntu

# Create database
createdb client_management_db
```

**Redis:**
```bash
# Install Redis
brew install redis  # macOS
# or
sudo apt-get install redis-server  # Ubuntu

# Start Redis
redis-server
```

### 2. Application Setup

1. **Install dependencies**:
   ```bash
   cd app
   npm install
   ```

2. **Environment configuration**:
   ```bash
   cp .env.example .env.local
   # Edit .env.local with your settings
   ```

3. **Database setup**:
   ```bash
   # Generate Prisma client
   npm run db:generate
   
   # Run migrations
   npm run db:migrate
   
   # Seed database
   npm run db:seed
   ```

4. **Build and start**:
   ```bash
   # Development
   npm run dev
   
   # Production
   npm run build
   npm start
   ```

## Configuration

### Environment Variables

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `DATABASE_URL` | PostgreSQL connection string | ✅ | - |
| `REDIS_URL` | Redis connection string | ✅ | - |
| `NEXTAUTH_SECRET` | NextAuth.js secret key | ✅ | - |
| `NEXTAUTH_URL` | Application base URL | ✅ | - |
| `EMAIL_FROM` | From email address | ✅ | - |
| `EMAIL_HOST` | SMTP host | ✅ | - |
| `EMAIL_PORT` | SMTP port | ✅ | 587 |
| `EMAIL_USER` | SMTP username | ✅ | - |
| `EMAIL_PASS` | SMTP password | ✅ | - |
| `UPLOAD_MAX_SIZE` | Max file size (bytes) | ❌ | 10485760 |
| `UPLOAD_DIR` | Upload directory | ❌ | ./uploads |
| `IMAGE_QUALITY` | Image compression quality | ❌ | 80 |

### Email Setup

The system supports various email providers:

**Gmail:**
```bash
EMAIL_HOST="smtp.gmail.com"
EMAIL_PORT="587"
EMAIL_USER="your-email@gmail.com" 
EMAIL_PASS="your-app-password"  # Use App Password, not regular password
```

**SendGrid:**
```bash
EMAIL_HOST="smtp.sendgrid.net"
EMAIL_PORT="587"
EMAIL_USER="apikey"
EMAIL_PASS="your-sendgrid-api-key"
```

**Custom SMTP:**
```bash
EMAIL_HOST="your-smtp-server.com"
EMAIL_PORT="587"
EMAIL_USER="your-username"
EMAIL_PASS="your-password"
```

## Security Setup

### 1. Generate Secure Secrets

```bash
# Generate NextAuth secret
openssl rand -base64 32

# Generate JWT secret  
openssl rand -base64 64
```

### 2. SSL/TLS (Production)

Update `NEXTAUTH_URL` to use HTTPS:
```bash
NEXTAUTH_URL="https://yourdomain.com"
```

### 3. Environment Security

- Never commit `.env.local` to version control
- Use environment-specific files (`.env.production`, `.env.staging`)
- Rotate secrets regularly

## Database Schema

The system uses the following main entities:

- **Users** - Authentication and user management
- **ClientProfiles** - Client information and preferences  
- **Projects** - Project tracking and management
- **ProjectFiles** - File uploads and processing
- **EmailLogs** - Email delivery tracking
- **AuditLogs** - System activity logging

## File Upload Configuration

### Storage Setup

1. **Local storage** (default):
   ```bash
   UPLOAD_DIR="./uploads"
   ```

2. **Docker volumes**:
   ```yaml
   volumes:
     - uploads_data:/app/uploads
   ```

### Image Processing

The system automatically:
- Optimizes images (JPEG, PNG, WebP)
- Generates thumbnails
- Categorizes by content
- Stores metadata

Supported formats:
- Images: JPEG, PNG, WebP, GIF, SVG
- Documents: PDF, TXT, DOC, DOCX
- Archives: ZIP, RAR, 7Z

## Testing

### Run Tests

```bash
# Unit tests
npm test

# Watch mode
npm run test:watch

# Coverage report
npm run test:coverage
```

### Test Database

For testing, use a separate database:
```bash
DATABASE_URL="postgresql://postgres:postgres@localhost:5432/client_management_test_db"
```

## Monitoring

### Health Checks

- Application: `GET /api/health`
- Database: Prisma connection status
- Redis: Cache connectivity
- Email: SMTP configuration

### Logging

The system logs to:
- Console (development)
- Database (audit logs)
- External services (production)

## Troubleshooting

### Common Issues

1. **Database connection failed**:
   - Verify PostgreSQL is running
   - Check `DATABASE_URL` format
   - Ensure database exists

2. **Redis connection failed**:
   - Verify Redis is running
   - Check `REDIS_URL` format
   - Test with `redis-cli ping`

3. **Email sending failed**:
   - Verify SMTP credentials
   - Check firewall/network access
   - Test with email provider

4. **File upload failed**:
   - Check upload directory permissions
   - Verify `UPLOAD_MAX_SIZE` setting
   - Ensure sufficient disk space

### Debug Mode

Enable debug logging:
```bash
LOG_LEVEL="debug"
NODE_ENV="development"
```

### Performance Issues

1. **Database slow queries**:
   - Check indexes
   - Analyze query performance
   - Consider connection pooling

2. **Redis memory usage**:
   - Monitor cache size
   - Configure eviction policies
   - Set appropriate TTL values

3. **File processing slow**:
   - Optimize image processing settings
   - Consider async processing
   - Monitor disk I/O

## Support

For technical support:
- Documentation: `/docs`
- Issues: GitHub Issues
- Email: support@company.com

## Security Considerations

- Use HTTPS in production
- Implement rate limiting
- Regular security audits
- Keep dependencies updated
- Monitor for vulnerabilities
- Backup data regularly