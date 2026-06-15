-- Test Setup Script for MemberLifecycleE2ETest
-- Creates the admin user required by the test authentication

-- First, delete the existing admin user if it exists
DELETE FROM common.user_permissions WHERE user_id = (SELECT id FROM common.users WHERE user_name = 'admin');

-- Delete the existing admin user if it exists
DELETE FROM common.users WHERE user_name = 'admin';

-- Insert admin user
INSERT INTO common.users (
    id,
    user_name,
    password_hash,
    account_status
) VALUES (
    '550e8400-e29b-41d4-a716-446655440000', -- Fixed UUID for test
    'admin',
    '$2b$10$y53O6KQAich9fC3pVAhO3OpfjVcpdZOuQNRFqMTjwERUXYVbcZ34a', -- password: admin123 (bcrypt)
    'ACTIVE'
);

-- Insert authorities for admin user
INSERT INTO common.user_permissions (user_id, authorities) VALUES (
    '550e8400-e29b-41d4-a716-446655440000',
    '["CALENDAR:MANAGE","MEMBERS:CREATE","MEMBERS:READ","MEMBERS:UPDATE","MEMBERS:DELETE","MEMBERS:PERMISSIONS","EVENTS:READ","EVENTS:MANAGE"]'
);
