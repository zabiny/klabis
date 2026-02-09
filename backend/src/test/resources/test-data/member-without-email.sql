-- Test data: Member without email for UserInfo integration tests
-- Creates User + Member + UserPermissions with NULL email (uses guardian email)

-- Clean up existing test data
DELETE FROM user_permissions WHERE user_id = '33333333-3333-3333-3333-333333333333';
DELETE FROM user_authorities WHERE user_id = '33333333-3333-3333-3333-333333333333';
DELETE FROM users WHERE id = '33333333-3333-3333-3333-333333333333';
DELETE FROM members WHERE id = '33333333-3333-3333-3333-333333333333';

-- Insert Member entity with NO email (minor with guardian)
INSERT INTO members (
    id,
    registration_number,
    first_name,
    last_name,
    date_of_birth,
    nationality,
    gender,
    email,  -- NULL - member has no email
    phone,  -- NULL - member has no phone
    street,
    city,
    postal_code,
    country,
    guardian_first_name,
    guardian_last_name,
    guardian_relationship,
    guardian_email,
    guardian_phone,
    is_active,
    created_at,
    created_by,
    modified_at,
    modified_by,
    version
) VALUES (
    '33333333-3333-3333-3333-333333333333',
    'ZBM0803',
    'Petra',
    'Mala',
    '2010-08-15',  -- Minor (under 18)
    'CZE',
    'FEMALE',
    NULL,  -- Member has NO email
    NULL,  -- Member has NO phone
    'Skolni 789',
    'Ostrava',
    '70200',
    'CZ',
    'Anna',
    'Mala',
    'MOTHER',
    'anna.mala@example.com',  -- Guardian has email
    '+420777888999',          -- Guardian has phone
    TRUE,
    CURRENT_TIMESTAMP,
    'test-setup',
    CURRENT_TIMESTAMP,
    'test-setup',
    0
);

-- Insert User entity (password: 'password123', BCrypt hashed)
INSERT INTO users (
    id,
    registration_number,
    password_hash,
    account_status,
    account_non_expired,
    account_non_locked,
    credentials_non_expired,
    enabled
) VALUES (
    '33333333-3333-3333-3333-333333333333',
    'ZBM0803',
    '$2a$10$N9qo8uLOickgx2ZMRZoMye8l/jDqmZuKlW/Lzx0rJKF/KJYwN/xHe',  -- 'password123'
    'ACTIVE',
    TRUE,
    TRUE,
    TRUE,
    TRUE
);

-- Insert user_authorities (for Spring Security authentication)
INSERT INTO user_authorities (user_id, authority) VALUES
    ('33333333-3333-3333-3333-333333333333', 'MEMBERS:READ');

-- Insert UserPermissions (application-level permissions)
INSERT INTO user_permissions (
    user_id,
    authorities,
    created_at,
    modified_at
) VALUES (
    '33333333-3333-3333-3333-333333333333',
    '["MEMBERS:READ"]',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
