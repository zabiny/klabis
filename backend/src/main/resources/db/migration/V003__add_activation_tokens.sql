ALTER TABLE users
    ADD COLUMN activation_token VARCHAR(255);
ALTER TABLE users
    ADD COLUMN activation_token_expires_at TIMESTAMP;
ALTER TABLE users
    ADD COLUMN activated_at TIMESTAMP;

-- Index for fast token lookup during activation
CREATE INDEX idx_users_activation_token ON users (activation_token);

-- Comment on columns for documentation
COMMENT ON COLUMN users.activation_token IS 'Secure random token sent in activation email (UUID format)';
COMMENT ON COLUMN users.activation_token_expires_at IS 'Token expiration timestamp (typically 72 hours after generation)';
COMMENT ON COLUMN users.activated_at IS 'Timestamp when account was activated via email link';
