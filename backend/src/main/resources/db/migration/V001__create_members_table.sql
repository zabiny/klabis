-- Create members table
-- Stores member registration and personal information

CREATE TABLE members
(
    id                    UUID PRIMARY KEY,
    registration_number   VARCHAR(7)   NOT NULL UNIQUE,

    -- Personal information
    first_name            VARCHAR(100) NOT NULL,
    last_name             VARCHAR(100) NOT NULL,
    date_of_birth         DATE         NOT NULL,
    nationality           VARCHAR(3)   NOT NULL,
    gender                VARCHAR(10)  NOT NULL,

    -- Contact information
    email                 VARCHAR(255), -- Member's primary email address (validated at application layer)
    phone                 VARCHAR(50),  -- Member's primary phone number (validated at application layer)

    -- Address information
    street                VARCHAR(200), -- Street address
    city                  VARCHAR(100), -- City name
    postal_code           VARCHAR(20),  -- Postal or ZIP code
    country               VARCHAR(2),   -- Country code (ISO 3166-1 alpha-2 format, e.g., 'CZ', 'US')

    -- Guardian information (for minors)
    guardian_first_name   VARCHAR(100),
    guardian_last_name    VARCHAR(100),
    guardian_relationship VARCHAR(50),
    guardian_email        VARCHAR(255),
    guardian_phone        VARCHAR(50),

    -- Status
    is_active             BOOLEAN      NOT NULL DEFAULT TRUE,

    -- Audit fields
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(100) NOT NULL,
    modified_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by           VARCHAR(100) NOT NULL,
    version               BIGINT       NOT NULL DEFAULT 0
);

-- Indexes
CREATE INDEX idx_members_registration_number ON members (registration_number);
CREATE INDEX idx_members_last_name ON members (last_name);
CREATE INDEX idx_members_date_of_birth ON members (date_of_birth);
CREATE INDEX idx_members_is_active ON members (is_active);

-- Comments
COMMENT ON TABLE members IS 'Stores club member registration and personal information';
COMMENT ON COLUMN members.registration_number IS 'Unique registration number in format XXXYYDD (club code + birth year + sequence)';
COMMENT ON COLUMN members.email IS 'Member primary email address (validated at application layer)';
COMMENT ON COLUMN members.phone IS 'Member primary phone number (validated at application layer)';
COMMENT ON COLUMN members.street IS 'Street address (nullable)';
COMMENT ON COLUMN members.city IS 'City name (nullable)';
COMMENT ON COLUMN members.postal_code IS 'Postal or ZIP code (nullable)';
COMMENT ON COLUMN members.country IS 'Country code using ISO 3166-1 alpha-2 format (nullable, e.g., CZ, US, SK)';
COMMENT ON COLUMN members.is_active IS 'Soft delete flag - false means member is deactivated';
