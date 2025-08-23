-- Insert default admin user
INSERT INTO users (
    username, 
    email, 
    password, 
    first_name, 
    last_name, 
    is_enabled, 
    is_account_non_expired, 
    is_account_non_locked, 
    is_credentials_non_expired,
    created_by
) VALUES (
    'admin',
    'admin@enterprise.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqcsdQbeeps2oHfvZJhUMd.', -- password: admin123
    'System',
    'Administrator',
    true,
    true,
    true,
    true,
    'system'
);

-- Insert admin role
INSERT INTO user_roles (user_id, role) 
SELECT user_id, 'ADMIN' FROM users WHERE username = 'admin';

-- Insert default test user
INSERT INTO users (
    username, 
    email, 
    password, 
    first_name, 
    last_name, 
    is_enabled, 
    is_account_non_expired, 
    is_account_non_locked, 
    is_credentials_non_expired,
    created_by
) VALUES (
    'testuser',
    'testuser@enterprise.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqcsdQbeeps2oHfvZJhUMd.', -- password: admin123
    'Test',
    'User',
    true,
    true,
    true,
    true,
    'system'
);

-- Insert user role
INSERT INTO user_roles (user_id, role) 
SELECT user_id, 'USER' FROM users WHERE username = 'testuser';

-- Insert default web client
INSERT INTO oauth2_clients (
    client_id,
    client_secret,
    client_name,
    access_token_validity_seconds,
    refresh_token_validity_seconds,
    require_authorization_consent,
    require_proof_key,
    client_authentication_methods,
    authorization_code_time_to_live,
    device_code_time_to_live,
    is_active,
    rate_limit_per_hour,
    created_by,
    description,
    contact_email
) VALUES (
    'web-client',
    '$2a$12$qyS5NyqY8YMBV7cSPLLV6OzW1dEJqZL2t3oLqL8NjKpJ4j2N7k8Rm', -- secret: web-secret
    'Enterprise Web Application',
    900,    -- 15 minutes
    604800, -- 7 days
    true,
    true,
    'client_secret_basic,client_secret_post',
    600,    -- 10 minutes
    1800,   -- 30 minutes
    true,
    1000,
    'system',
    'Default web client for OAuth2 testing',
    'admin@enterprise.com'
);

-- Insert redirect URIs for web client
INSERT INTO oauth2_client_redirect_uris (client_id, redirect_uri) VALUES
('web-client', 'http://localhost:3000/callback'),
('web-client', 'http://localhost:3000/api/auth/callback');

-- Insert grant types for web client
INSERT INTO oauth2_client_grant_types (client_id, grant_type) VALUES
('web-client', 'authorization_code'),
('web-client', 'refresh_token');

-- Insert scopes for web client
INSERT INTO oauth2_client_scopes (client_id, scope) VALUES
('web-client', 'openid'),
('web-client', 'profile'),
('web-client', 'email'),
('web-client', 'read'),
('web-client', 'write');

-- Insert default service client
INSERT INTO oauth2_clients (
    client_id,
    client_secret,
    client_name,
    access_token_validity_seconds,
    refresh_token_validity_seconds,
    require_authorization_consent,
    require_proof_key,
    client_authentication_methods,
    authorization_code_time_to_live,
    device_code_time_to_live,
    is_active,
    rate_limit_per_hour,
    created_by,
    description,
    contact_email
) VALUES (
    'service-client',
    '$2a$12$qyS5NyqY8YMBV7cSPLLV6OzW1dEJqZL2t3oLqL8NjKpJ4j2N7k8Rm', -- secret: service-secret
    'Enterprise Service Client',
    3600,   -- 1 hour
    null,   -- no refresh token for client credentials
    false,
    false,
    'client_secret_basic',
    null,   -- no authorization code for client credentials
    null,   -- no device code for client credentials
    true,
    5000,
    'system',
    'Service-to-service authentication client',
    'admin@enterprise.com'
);

-- Insert grant types for service client
INSERT INTO oauth2_client_grant_types (client_id, grant_type) VALUES
('service-client', 'client_credentials');

-- Insert scopes for service client
INSERT INTO oauth2_client_scopes (client_id, scope) VALUES
('service-client', 'service'),
('service-client', 'admin');

-- Insert default device client
INSERT INTO oauth2_clients (
    client_id,
    client_secret,
    client_name,
    access_token_validity_seconds,
    refresh_token_validity_seconds,
    require_authorization_consent,
    require_proof_key,
    client_authentication_methods,
    authorization_code_time_to_live,
    device_code_time_to_live,
    is_active,
    rate_limit_per_hour,
    created_by,
    description,
    contact_email
) VALUES (
    'device-client',
    null, -- no secret for public device clients
    'Enterprise Device Client',
    1800,   -- 30 minutes
    86400,  -- 1 day
    true,
    false,
    'none',
    null,   -- no authorization code for device flow
    1800,   -- 30 minutes
    true,
    100,
    'system',
    'Device flow client for limited input devices',
    'admin@enterprise.com'
);

-- Insert grant types for device client
INSERT INTO oauth2_client_grant_types (client_id, grant_type) VALUES
('device-client', 'urn:ietf:params:oauth:grant-type:device_code'),
('device-client', 'refresh_token');

-- Insert scopes for device client
INSERT INTO oauth2_client_scopes (client_id, scope) VALUES
('device-client', 'openid'),
('device-client', 'profile'),
('device-client', 'read');