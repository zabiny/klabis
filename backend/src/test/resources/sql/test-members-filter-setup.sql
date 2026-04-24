-- Test member data for MemberFilterE2ETest
-- Provides a controlled set of members with varied names and active/inactive state
-- to verify fulltext (q) and status filter combinations.

DELETE FROM members
WHERE id IN (
    '00000001-0001-0001-0001-000000000001',
    '00000001-0001-0001-0001-000000000002',
    '00000001-0001-0001-0001-000000000003',
    '00000001-0001-0001-0001-000000000004',
    '00000001-0001-0001-0001-000000000099'
);

-- Calling admin user — required so the JWT member claim resolves to a real member row
INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender,
                     email, phone, street, city, postal_code, country, is_active,
                     created_at, created_by, modified_at, modified_by, version, chip_number)
VALUES ('00000001-0001-0001-0001-000000000099',
        'ZBM0099', 'Admin', 'User', '1980-01-01', 'SK', 'MALE',
        'admin@example.com', '+420111000000', 'Admin St 1', 'Prague', '10000', 'CZ', TRUE,
        CURRENT_TIMESTAMP, 'test-setup', CURRENT_TIMESTAMP, 'test-setup', 0, NULL);

-- Active member: Jan Novak
INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender,
                     email, phone, street, city, postal_code, country, is_active,
                     created_at, created_by, modified_at, modified_by, version, chip_number)
VALUES ('00000001-0001-0001-0001-000000000001',
        'ZBM0001', 'Jan', 'Novak', '1990-03-15', 'SK', 'MALE',
        'jan.novak@example.com', '+420123456001', 'Hlavni 1', 'Praha', '11000', 'CZ', TRUE,
        CURRENT_TIMESTAMP, 'test-setup', CURRENT_TIMESTAMP, 'test-setup', 0, NULL);

-- Active member: Petra Cermakova (diacritics in last name — tests unaccent)
INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender,
                     email, phone, street, city, postal_code, country, is_active,
                     created_at, created_by, modified_at, modified_by, version, chip_number)
VALUES ('00000001-0001-0001-0001-000000000002',
        'ZBM0002', 'Petra', 'Čermáková', '1992-07-22', 'CZ', 'FEMALE',
        'petra.cermakova@example.com', '+420123456002', 'Namesti 2', 'Brno', '60200', 'CZ', TRUE,
        CURRENT_TIMESTAMP, 'test-setup', CURRENT_TIMESTAMP, 'test-setup', 0, NULL);

-- Active member: Karel Novak (same last name as Jan — tiebreak by firstName)
INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender,
                     email, phone, street, city, postal_code, country, is_active,
                     created_at, created_by, modified_at, modified_by, version, chip_number)
VALUES ('00000001-0001-0001-0001-000000000003',
        'ZBM0003', 'Karel', 'Novak', '1988-11-05', 'CZ', 'MALE',
        'karel.novak@example.com', '+420123456003', 'Sidliste 3', 'Ostrava', '70800', 'CZ', TRUE,
        CURRENT_TIMESTAMP, 'test-setup', CURRENT_TIMESTAMP, 'test-setup', 0, NULL);

-- Inactive member: Jana Novakova
INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender,
                     email, phone, street, city, postal_code, country, is_active,
                     created_at, created_by, modified_at, modified_by, version, chip_number)
VALUES ('00000001-0001-0001-0001-000000000004',
        'ZBM0004', 'Jana', 'Nováková', '1995-04-30', 'SK', 'FEMALE',
        'jana.novakova@example.com', '+420123456004', 'Kopecka 4', 'Plzen', '30100', 'CZ', FALSE,
        CURRENT_TIMESTAMP, 'test-setup', CURRENT_TIMESTAMP, 'test-setup', 0, NULL);
