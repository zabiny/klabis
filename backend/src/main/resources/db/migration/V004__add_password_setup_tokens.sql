-- Create password_setup_tokens table for user account activation
CREATE TABLE password_setup_tokens
(
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP   NOT NULL,
    used_at    TIMESTAMP,
    used_by_ip VARCHAR(45),
    CONSTRAINT unique_user_token UNIQUE (user_id, created_at)
);

-- Create indexes for efficient queries
CREATE INDEX idx_password_tokens_user_id ON password_setup_tokens (user_id);
CREATE INDEX idx_password_tokens_token_hash ON password_setup_tokens (token_hash);
CREATE INDEX idx_password_tokens_expires_at ON password_setup_tokens (expires_at);
CREATE INDEX idx_password_tokens_created_at ON password_setup_tokens (created_at);

-- Comments for documentation
COMMENT ON TABLE password_setup_tokens IS 'Tokens for user self-service password setup during account activation';
COMMENT ON COLUMN password_setup_tokens.id IS 'Unique token identifier (UUID)';
COMMENT ON COLUMN password_setup_tokens.user_id IS 'Reference to user account requiring password setup';
COMMENT ON COLUMN password_setup_tokens.token_hash IS 'SHA-256 hash of the random token (never store plain text)';
COMMENT ON COLUMN password_setup_tokens.created_at IS 'Token generation timestamp';
COMMENT ON COLUMN password_setup_tokens.expires_at IS 'Token expiration timestamp (typically 4 hours after generation)';
COMMENT ON COLUMN password_setup_tokens.used_at IS 'Timestamp when token was used to set password (null if unused)';
COMMENT ON COLUMN password_setup_tokens.used_by_ip IS 'IP address of user who set password (for security audit)';
