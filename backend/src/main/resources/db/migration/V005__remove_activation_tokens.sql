-- Drop old activation token columns from users table (replaced by password_setup_tokens)
ALTER TABLE users
    DROP COLUMN IF EXISTS activation_token;
ALTER TABLE users
    DROP COLUMN IF EXISTS activation_token_expires_at;
ALTER TABLE users
    DROP COLUMN IF EXISTS activated_at;

-- Drop the old activation token index
DROP INDEX IF EXISTS idx_users_activation_token;
