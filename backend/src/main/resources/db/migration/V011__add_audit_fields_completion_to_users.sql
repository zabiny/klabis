-- Complete audit fields for users table
-- Add created_by and last_modified_by columns for parity with members table
-- Users table already has created_at and modified_at from V008

-- Add created_by field (who created the user account)
ALTER TABLE users
    ADD COLUMN created_by VARCHAR(100);

-- Add last_modified_by field (who last modified the user account)
ALTER TABLE users
    ADD COLUMN last_modified_by VARCHAR(100);

-- Set default value for existing rows
-- System-created users will have 'SYSTEM' as creator
UPDATE users
SET created_by = 'SYSTEM'
WHERE created_by IS NULL;
UPDATE users
SET last_modified_by = 'SYSTEM'
WHERE last_modified_by IS NULL;
