-- Test member data for event registration E2E tests
-- This script creates a test member that corresponds to the authenticated user ID
-- used in @WithMockUser annotations

-- Clean up any existing test data
DELETE
FROM members
WHERE id = '11111111-1111-1111-1111-111111111111';

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
VALUES ('11111111-1111-1111-1111-111111111111', -- User ID used in @WithMockUser
        'ZBM0001', -- Registration number
        'Test', -- First name
        'User', -- Last name
        '1990-01-15', -- Date of birth
        'CZE', -- Nationality (ISO 3166-1 alpha-3)
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
