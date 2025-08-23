-- JWT Token Management Tables

-- RSA Key Pairs Table
CREATE TABLE rsa_key_pairs (
    id BIGSERIAL PRIMARY KEY,
    key_id VARCHAR(255) UNIQUE NOT NULL,
    algorithm VARCHAR(50) NOT NULL DEFAULT 'RS256',
    key_size INTEGER NOT NULL DEFAULT 2048,
    public_key TEXT NOT NULL,
    private_key_encrypted TEXT NOT NULL,
    encryption_key_id VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    deactivated_at TIMESTAMP
);

-- JWT Tokens Table
CREATE TABLE jwt_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_id VARCHAR(255) UNIQUE NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    client_id VARCHAR(255) NOT NULL,
    token_type VARCHAR(50) NOT NULL CHECK (token_type IN ('ACCESS_TOKEN', 'REFRESH_TOKEN', 'ID_TOKEN')),
    token_hash VARCHAR(512) NOT NULL,
    audience VARCHAR(255) NOT NULL,
    issuer VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    not_before TIMESTAMP,
    revoked BOOLEAN NOT NULL DEFAULT false,
    revoked_at TIMESTAMP,
    revoked_reason VARCHAR(500),
    last_used_at TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- JWT Token Scopes Table (Many-to-Many relationship)
CREATE TABLE jwt_token_scopes (
    token_id BIGINT NOT NULL REFERENCES jwt_tokens(id) ON DELETE CASCADE,
    scope VARCHAR(100) NOT NULL,
    PRIMARY KEY (token_id, scope)
);

-- Token Blacklist Table
CREATE TABLE token_blacklist (
    id BIGSERIAL PRIMARY KEY,
    token_id VARCHAR(255) UNIQUE NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    token_type VARCHAR(50) NOT NULL CHECK (token_type IN ('ACCESS_TOKEN', 'REFRESH_TOKEN', 'ID_TOKEN')),
    revoked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    reason VARCHAR(500),
    revoked_by VARCHAR(255),
    ip_address VARCHAR(45)
);

-- Indexes for performance optimization

-- RSA Key Pairs indexes
CREATE INDEX idx_rsa_key_id ON rsa_key_pairs(key_id);
CREATE INDEX idx_rsa_active ON rsa_key_pairs(active);
CREATE INDEX idx_rsa_expires_at ON rsa_key_pairs(expires_at);

-- JWT Tokens indexes
CREATE INDEX idx_jwt_token_id ON jwt_tokens(token_id);
CREATE INDEX idx_jwt_user_id ON jwt_tokens(user_id);
CREATE INDEX idx_jwt_client_id ON jwt_tokens(client_id);
CREATE INDEX idx_jwt_expires_at ON jwt_tokens(expires_at);
CREATE INDEX idx_jwt_revoked ON jwt_tokens(revoked);
CREATE INDEX idx_jwt_token_type ON jwt_tokens(token_type);
CREATE INDEX idx_jwt_user_type ON jwt_tokens(user_id, token_type);
CREATE INDEX idx_jwt_user_revoked ON jwt_tokens(user_id, revoked);
CREATE INDEX idx_jwt_expires_revoked ON jwt_tokens(expires_at, revoked);

-- Token Blacklist indexes
CREATE INDEX idx_blacklist_token_id ON token_blacklist(token_id);
CREATE INDEX idx_blacklist_expires_at ON token_blacklist(expires_at);
CREATE INDEX idx_blacklist_user_id ON token_blacklist(user_id);
CREATE INDEX idx_blacklist_revoked_at ON token_blacklist(revoked_at);

-- Add updated_at trigger for jwt_tokens table
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_jwt_tokens_updated_at 
    BEFORE UPDATE ON jwt_tokens 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Comments for documentation
COMMENT ON TABLE rsa_key_pairs IS 'RSA key pairs used for JWT signing and verification';
COMMENT ON TABLE jwt_tokens IS 'JWT tokens issued by the authentication service';
COMMENT ON TABLE jwt_token_scopes IS 'Scopes associated with JWT tokens';
COMMENT ON TABLE token_blacklist IS 'Blacklisted tokens for logout and revocation';

COMMENT ON COLUMN rsa_key_pairs.key_id IS 'Unique identifier for the key pair (kid claim)';
COMMENT ON COLUMN rsa_key_pairs.private_key_encrypted IS 'Encrypted private key for security';
COMMENT ON COLUMN jwt_tokens.token_hash IS 'Hash of the JWT token for database lookup';
COMMENT ON COLUMN jwt_tokens.token_type IS 'Type of JWT token (ACCESS_TOKEN, REFRESH_TOKEN, ID_TOKEN)';
COMMENT ON COLUMN token_blacklist.expires_at IS 'When the blacklist entry expires (same as original token expiry)';