-- Test member data for event registration E2E tests
-- This script creates test members that correspond to the authenticated user IDs
-- used in @WithMockUser annotations

-- Clean up any existing test data
DELETE
FROM members
WHERE id IN ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222');

-- Insert test member
INSERT INTO members (id,
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
                     version,
                     chip_number)
VALUES ('11111111-1111-1111-1111-111111111111', -- User ID 1
        'ZBM0001', -- Registration number
        'Test', -- First name
        'User', -- Last name
        '1990-01-15', -- Date of birth
        'SK', -- Nationality (ISO 3166-1 alpha-2) - using SK to avoid birth number requirement
        'MALE', -- Gender
        'test.user@example.com', -- Email
        '+420123456789', -- Phone
        'Test Street 123', -- Street
        'Prague', -- City
        '12000', -- Postal code
        'CZ', -- Country (ISO 3166-1 alpha-2)
        TRUE, -- is_active
        CURRENT_TIMESTAMP, -- created_at
        'test-setup', -- created_by
        CURRENT_TIMESTAMP, -- modified_at
        'test-setup', -- modified_by
        0, -- version
        NULL -- chip_number (optional)
       );

-- Insert second test member
INSERT INTO members (id,
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
                     version,
                     chip_number)
VALUES ('22222222-2222-2222-2222-222222222222', -- User ID 2
        'ZBM0002', -- Registration number
        'Second', -- First name
        'User', -- Last name
        '1985-05-20', -- Date of birth
        'SK', -- Nationality (ISO 3166-1 alpha-2)
        'FEMALE', -- Gender
        'second.user@example.com', -- Email
        '+42123456789', -- Phone
        'Second Street 456', -- Street
        'Bratislava', -- City
        '82109', -- Postal code
        'SK', -- Country (ISO 3166-1 alpha-2)
        TRUE, -- is_active
        CURRENT_TIMESTAMP, -- created_at
        'test-setup', -- created_by
        CURRENT_TIMESTAMP, -- modified_at
        'test-setup', -- modified_by
        0, -- version
        NULL -- chip_number (optional)
       );
