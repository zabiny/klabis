-- Add audit fields to users table for UserEntity
-- These fields support JPA auditing (@CreatedDate, @LastModifiedDate, @Version)

-- Add created_at timestamp
ALTER TABLE users
    ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Add modified_at timestamp
ALTER TABLE users
    ADD COLUMN modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Add version for optimistic locking
ALTER TABLE users
    ADD COLUMN version BIGINT DEFAULT 0;

-- Create index on created_at for common queries
CREATE INDEX idx_users_created_at ON users (created_at);
