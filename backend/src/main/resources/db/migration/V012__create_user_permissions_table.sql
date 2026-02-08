-- Create user_permissions table to separate authorization from authentication
-- This table stores direct authorities for each user as a JSON array string
-- Authorities are managed separately from User entity for better separation of concerns

CREATE TABLE user_permissions
(
    user_id     UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    authorities VARCHAR(1000) NOT NULL, -- JSON array of authority strings: ["MEMBERS:READ", "TRAINING:VIEW"]
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index for faster queries by user_id
CREATE INDEX idx_user_permissions_user_id ON user_permissions (user_id);

-- Add comment for documentation
COMMENT ON TABLE user_permissions IS 'User permissions/authorities, separated from User entity for authorization';
COMMENT ON COLUMN user_permissions.user_id IS 'Reference to user (UUID)';
COMMENT ON COLUMN user_permissions.authorities IS 'Direct authorities as JSON array string (e.g., ["MEMBERS:READ", "TRAINING:VIEW"])';
COMMENT ON COLUMN user_permissions.created_at IS 'Timestamp when permissions were created';
COMMENT ON COLUMN user_permissions.modified_at IS 'Timestamp when permissions were last modified';
