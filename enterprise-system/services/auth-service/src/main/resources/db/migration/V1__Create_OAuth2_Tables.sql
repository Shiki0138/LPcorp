-- OAuth2 Authorization Server Tables

-- Users table
CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone_number VARCHAR(20),
    is_enabled BOOLEAN DEFAULT true,
    is_account_non_expired BOOLEAN DEFAULT true,
    is_account_non_locked BOOLEAN DEFAULT true,
    is_credentials_non_expired BOOLEAN DEFAULT true,
    failed_login_attempts INTEGER DEFAULT 0,
    last_login_at TIMESTAMP WITH TIME ZONE,
    password_changed_at TIMESTAMP WITH TIME ZONE,
    account_locked_at TIMESTAMP WITH TIME ZONE,
    is_mfa_enabled BOOLEAN DEFAULT false,
    totp_secret VARCHAR(100),
    backup_codes VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    organization_id VARCHAR(100),
    department_id VARCHAR(100),
    last_login_ip VARCHAR(45),
    last_login_user_agent VARCHAR(500),
    timezone VARCHAR(50) DEFAULT 'UTC',
    locale VARCHAR(10) DEFAULT 'en'
);

-- User roles table
CREATE TABLE user_roles (
    user_id BIGINT REFERENCES users(user_id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- OAuth2 clients table
CREATE TABLE oauth2_clients (
    client_id VARCHAR(100) PRIMARY KEY,
    client_secret VARCHAR(200) NOT NULL,
    client_name VARCHAR(200) NOT NULL,
    access_token_validity_seconds INTEGER,
    refresh_token_validity_seconds INTEGER,
    require_authorization_consent BOOLEAN DEFAULT false,
    require_proof_key BOOLEAN DEFAULT false,
    client_authentication_methods VARCHAR(500),
    authorization_code_time_to_live INTEGER,
    device_code_time_to_live INTEGER,
    is_active BOOLEAN DEFAULT true,
    rate_limit_per_hour INTEGER DEFAULT 1000,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    description VARCHAR(1000),
    contact_email VARCHAR(255),
    website_url VARCHAR(500)
);

-- OAuth2 client redirect URIs table
CREATE TABLE oauth2_client_redirect_uris (
    client_id VARCHAR(100) REFERENCES oauth2_clients(client_id) ON DELETE CASCADE,
    redirect_uri VARCHAR(500) NOT NULL,
    PRIMARY KEY (client_id, redirect_uri)
);

-- OAuth2 client grant types table
CREATE TABLE oauth2_client_grant_types (
    client_id VARCHAR(100) REFERENCES oauth2_clients(client_id) ON DELETE CASCADE,
    grant_type VARCHAR(50) NOT NULL,
    PRIMARY KEY (client_id, grant_type)
);

-- OAuth2 client scopes table
CREATE TABLE oauth2_client_scopes (
    client_id VARCHAR(100) REFERENCES oauth2_clients(client_id) ON DELETE CASCADE,
    scope VARCHAR(100) NOT NULL,
    PRIMARY KEY (client_id, scope)
);

-- OAuth2 authorizations table
CREATE TABLE oauth2_authorizations (
    id VARCHAR(100) PRIMARY KEY,
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    authorization_grant_type VARCHAR(100) NOT NULL,
    authorized_scopes VARCHAR(1000),
    attributes TEXT,
    state VARCHAR(500),
    authorization_code_value TEXT,
    authorization_code_issued_at TIMESTAMP WITH TIME ZONE,
    authorization_code_expires_at TIMESTAMP WITH TIME ZONE,
    authorization_code_metadata VARCHAR(2000),
    access_token_value TEXT,
    access_token_issued_at TIMESTAMP WITH TIME ZONE,
    access_token_expires_at TIMESTAMP WITH TIME ZONE,
    access_token_metadata VARCHAR(2000),
    access_token_type VARCHAR(100),
    access_token_scopes VARCHAR(1000),
    refresh_token_value TEXT,
    refresh_token_issued_at TIMESTAMP WITH TIME ZONE,
    refresh_token_expires_at TIMESTAMP WITH TIME ZONE,
    refresh_token_metadata VARCHAR(2000),
    oidc_id_token_value TEXT,
    oidc_id_token_issued_at TIMESTAMP WITH TIME ZONE,
    oidc_id_token_expires_at TIMESTAMP WITH TIME ZONE,
    oidc_id_token_metadata VARCHAR(2000),
    user_code_value TEXT,
    user_code_issued_at TIMESTAMP WITH TIME ZONE,
    user_code_expires_at TIMESTAMP WITH TIME ZONE,
    user_code_metadata VARCHAR(2000),
    device_code_value TEXT,
    device_code_issued_at TIMESTAMP WITH TIME ZONE,
    device_code_expires_at TIMESTAMP WITH TIME ZONE,
    device_code_metadata VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500)
);

-- OAuth2 authorization consents table
CREATE TABLE oauth2_authorization_consents (
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    authorities VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    PRIMARY KEY (registered_client_id, principal_name)
);

-- Security events table
CREATE TABLE security_events (
    event_id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    username VARCHAR(100),
    client_id VARCHAR(100),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    success BOOLEAN,
    error_code VARCHAR(50),
    error_message VARCHAR(1000),
    session_id VARCHAR(100),
    resource VARCHAR(500),
    action VARCHAR(100),
    additional_data TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    severity VARCHAR(20),
    risk_score INTEGER,
    geo_location VARCHAR(200),
    device_fingerprint VARCHAR(200)
);

-- Indexes for better performance
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_enabled ON users(is_enabled);
CREATE INDEX idx_oauth2_clients_active ON oauth2_clients(is_active);
CREATE INDEX idx_oauth2_authorizations_client_id ON oauth2_authorizations(registered_client_id);
CREATE INDEX idx_oauth2_authorizations_principal ON oauth2_authorizations(principal_name);
CREATE INDEX idx_oauth2_authorizations_state ON oauth2_authorizations(state);
CREATE INDEX idx_oauth2_authorizations_auth_code ON oauth2_authorizations(authorization_code_value);
CREATE INDEX idx_oauth2_authorizations_access_token ON oauth2_authorizations(access_token_value);
CREATE INDEX idx_oauth2_authorizations_refresh_token ON oauth2_authorizations(refresh_token_value);
CREATE INDEX idx_oauth2_authorizations_id_token ON oauth2_authorizations(oidc_id_token_value);
CREATE INDEX idx_oauth2_authorizations_user_code ON oauth2_authorizations(user_code_value);
CREATE INDEX idx_oauth2_authorizations_device_code ON oauth2_authorizations(device_code_value);
CREATE INDEX idx_oauth2_authorizations_expires_at ON oauth2_authorizations(access_token_expires_at);
CREATE INDEX idx_security_events_username ON security_events(username);
CREATE INDEX idx_security_events_client_id ON security_events(client_id);
CREATE INDEX idx_security_events_ip_address ON security_events(ip_address);
CREATE INDEX idx_security_events_event_type ON security_events(event_type);
CREATE INDEX idx_security_events_created_at ON security_events(created_at);
CREATE INDEX idx_security_events_severity ON security_events(severity);
CREATE INDEX idx_security_events_risk_score ON security_events(risk_score);