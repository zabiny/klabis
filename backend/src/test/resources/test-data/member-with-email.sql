-- Test data: Member with email for UserInfo integration tests
-- Creates User + Member + UserPermissions with valid email address

-- Clean up existing test data
DELETE FROM user_permissions WHERE user_id = '22222222-2222-2222-2222-222222222222';
DELETE FROM users WHERE id = '22222222-2222-2222-2222-222222222222';
DELETE FROM members WHERE id = '22222222-2222-2222-2222-222222222222';

-- Insert Member entity (User and Member share same ID)
INSERT INTO members (
    id,
    registration_number,
    first_name,
    last_name,
    date_of_birth,
    nationality,
    gender,
    email,
    phone,
    street,
    city,
    postal_code,
    country,
    is_active,
    created_at,
    created_by,
    modified_at,
    modified_by,
    version
) VALUES (
    '22222222-2222-2222-2222-222222222222',
    'ZBM0502',
    'Jan',
    'Novak',
    '1995-03-20',
    'CZE',
    'MALE',
    'jan.novak@example.com',  -- Member HAS email
    '+420987654321',
    'Hlavni 456',
    'Brno',
    '60200',
    'CZ',
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
    '22222222-2222-2222-2222-222222222222',
    'ZBM0502',
    '$2a$10$N9qo8uLOickgx2ZMRZoMye8l/jDqmZuKlW/Lzx0rJKF/KJYwN/xHe',  -- 'password123'
    'ACTIVE',
    TRUE,
    TRUE,
    TRUE,
    TRUE
);

-- Insert UserPermissions (application-level permissions)
INSERT INTO user_permissions (
    user_id,
    authorities,
    created_at,
    modified_at
) VALUES (
    '22222222-2222-2222-2222-222222222222',
    '["MEMBERS:READ"]',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
