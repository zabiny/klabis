-- Add authorities column to users table
-- This column stores custom authorities as a JSON array string
-- Authorities are required and cannot be empty

-- ALTER TABLE users
-- ADD COLUMN authorities TEXT NOT NULL DEFAULT '["MEMBERS:READ"]';

-- Add constraint to ensure authorities is a valid non-empty JSON array
-- Validates JSON array structure without relying on database-specific JSON functions
-- Compatible with both H2 (dev/test) and PostgreSQL (production)
-- Ensures:
--   1. String starts with '[' and ends with ']' (JSON array format)
--   2. Not an empty array '[]' (at least one authority required)
--   3. Contains at least one quote character (has JSON string elements)
-- ALTER TABLE users
-- ADD CONSTRAINT chk_authorities_not_empty
-- CHECK (
--     authorities IS NOT NULL
--     AND TRIM(authorities) != ''
--     AND LENGTH(TRIM(authorities)) >= 6  -- Minimum: '["X"]' where X is any single char
--     AND SUBSTRING(TRIM(authorities), 1, 1) = '['
--     AND SUBSTRING(TRIM(authorities), LENGTH(TRIM(authorities)), 1) = ']'
--     AND LOCATE('"', TRIM(authorities)) > 0  -- Contains at least one quote (JSON string)
--     AND TRIM(authorities) != '[]'  -- Explicitly reject empty array
-- );

-- Add comment for documentation
-- COMMENT ON COLUMN users.authorities IS 'Custom authorities as JSON array string (e.g., ["MEMBERS:READ", "MEMBERS:CREATE"]). Required, cannot be empty.';
