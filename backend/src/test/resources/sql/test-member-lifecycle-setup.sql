-- Test Setup Script for MemberLifecycleE2ETest
-- Creates the admin user required by the test authentication

-- First, delete the existing admin user if it exists
DELETE FROM user_permissions WHERE user_id = (SELECT id FROM users WHERE user_name = 'admin');

-- Delete the existing admin user if it exists
DELETE FROM users WHERE user_name = 'admin';

-- Insert admin user
INSERT INTO users (
    id,
    user_name,
    password_hash,
    account_status,
    account_non_expired,
    account_non_locked,
    credentials_non_expired,
    enabled
) VALUES (
    '550e8400-e29b-41d4-a716-446655440000', -- Fixed UUID for test
    'admin',
    '$2b$10$y53O6KQAich9fC3pVAhO3OpfjVcpdZOuQNRFqMTjwERUXYVbcZ34a', -- password: admin123 (bcrypt)
    'ACTIVE',
    TRUE,
    TRUE,
    TRUE,
    TRUE
);

-- Insert authorities for admin user
INSERT INTO user_permissions (user_id, authorities) VALUES (
    '550e8400-e29b-41d4-a716-446655440000',
    '["CALENDAR:MANAGE","MEMBERS:CREATE","MEMBERS:READ","MEMBERS:UPDATE","MEMBERS:DELETE","MEMBERS:PERMISSIONS","EVENTS:READ","EVENTS:MANAGE"]'
);