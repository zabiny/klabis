-- Initial schema for Klabis backend
-- Created: 2026-02-09
-- Squashed from historical V001-V013 migrations
--
-- This single migration creates the complete database schema for a fresh database.
-- Bootstrap data (admin user, OAuth2 clients) managed by BootstrapDataLoader component
--
-- Tables in dependency order:
-- 1. members (no dependencies)
-- 2. users (no dependencies)
-- 3. user_permissions (FK → users)
-- 4. password_setup_tokens (FK → users)
-- 5. oauth2_registered_client (no dependencies)
-- 6. oauth2_authorization (no dependencies)
-- 7. oauth2_authorization_consent (no dependencies)
-- 8. event_publication (no dependencies)
-- 9. events (FK → members)
-- 10. event_registrations (FK → events, members)

-- ============================================================================
-- 1. MEMBERS TABLE
-- Stores member registration and personal information
-- ============================================================================

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

    -- Member self-edit fields (from V009)
    chip_number           VARCHAR(50),
    identity_card_number  VARCHAR(50),
    identity_card_validity_date DATE,
    medical_course_completion_date DATE,
    medical_course_validity_date DATE,
    trainer_license_number VARCHAR(50),
    trainer_license_validity_date DATE,
    driving_license_group VARCHAR(10),
    dietary_restrictions  VARCHAR(500),

    -- Audit fields
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(100) NOT NULL,
    modified_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by           VARCHAR(100) NOT NULL,
    version               BIGINT       NOT NULL DEFAULT 0
);

-- Indexes for members
CREATE INDEX idx_members_registration_number ON members (registration_number);
CREATE INDEX idx_members_last_name ON members (last_name);
CREATE INDEX idx_members_date_of_birth ON members (date_of_birth);
CREATE INDEX idx_members_is_active ON members (is_active);

-- Comments for members table
COMMENT ON TABLE members IS 'Stores club member registration and personal information';
COMMENT ON COLUMN members.registration_number IS 'Unique registration number in format XXXYYDD (club code + birth year + sequence)';
COMMENT ON COLUMN members.email IS 'Member primary email address (validated at application layer)';
COMMENT ON COLUMN members.phone IS 'Member primary phone number (validated at application layer)';
COMMENT ON COLUMN members.street IS 'Street address (nullable)';
COMMENT ON COLUMN members.city IS 'City name (nullable)';
COMMENT ON COLUMN members.postal_code IS 'Postal or ZIP code (nullable)';
COMMENT ON COLUMN members.country IS 'Country code using ISO 3166-1 alpha-2 format (nullable, e.g., CZ, US, SK)';
COMMENT ON COLUMN members.is_active IS 'Soft delete flag - false means member is deactivated';
COMMENT ON COLUMN members.chip_number IS 'Member identification chip number (nullable, max 50 characters)';
COMMENT ON COLUMN members.identity_card_number IS 'Identity card number (nullable, max 50 characters)';
COMMENT ON COLUMN members.identity_card_validity_date IS 'Identity card validity expiration date (nullable)';
COMMENT ON COLUMN members.medical_course_completion_date IS 'Medical course completion date (nullable)';
COMMENT ON COLUMN members.medical_course_validity_date IS 'Medical course validity expiration date (nullable)';
COMMENT ON COLUMN members.trainer_license_number IS 'Trainer license number (nullable, max 50 characters)';
COMMENT ON COLUMN members.trainer_license_validity_date IS 'Trainer license validity expiration date (nullable)';
COMMENT ON COLUMN members.driving_license_group IS 'Driving license group/category (nullable, max 10 characters, e.g., BE, C1, C1E)';
COMMENT ON COLUMN members.dietary_restrictions IS 'Dietary restrictions or food allergies (nullable, max 500 characters)';

-- ============================================================================
-- 2. USERS TABLE
-- User accounts for authentication
-- ============================================================================

CREATE TABLE users
(
    id                      UUID PRIMARY KEY,
    registration_number     VARCHAR(7)   NOT NULL UNIQUE,
    password_hash           VARCHAR(255) NOT NULL,
    account_status          VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    account_non_expired     BOOLEAN      NOT NULL DEFAULT TRUE,
    account_non_locked      BOOLEAN      NOT NULL DEFAULT TRUE,
    credentials_non_expired BOOLEAN      NOT NULL DEFAULT TRUE,
    enabled                 BOOLEAN      NOT NULL DEFAULT TRUE,

    -- Audit fields (from V008)
    created_at              TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    modified_at             TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    version                 BIGINT       DEFAULT 0,

    -- Audit creator fields (from V011)
    created_by              VARCHAR(100),
    last_modified_by        VARCHAR(100)
);

-- Indexes for users
CREATE INDEX idx_users_registration_number ON users (registration_number);
CREATE INDEX idx_users_account_status ON users (account_status);
CREATE INDEX idx_users_created_at ON users (created_at);

-- Comments for users table
COMMENT ON TABLE users IS 'User accounts for authentication';
COMMENT ON COLUMN users.registration_number IS 'Unique registration number, username for login';
COMMENT ON COLUMN users.password_hash IS 'BCrypt-hashed password';

-- ============================================================================
-- 3. USER_PERMISSIONS TABLE
-- User permissions/authorities, separated from User entity for authorization
-- Note: user_authorities table (V002) was deprecated and removed - authorization uses this table exclusively
-- ============================================================================

CREATE TABLE user_permissions
(
    user_id     UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    authorities VARCHAR(1000) NOT NULL, -- JSON array of authority strings: ["MEMBERS:READ", "TRAINING:VIEW"]
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for user_permissions
CREATE INDEX idx_user_permissions_user_id ON user_permissions (user_id);

-- Comments for user_permissions
COMMENT ON TABLE user_permissions IS 'User permissions/authorities, separated from User entity for authorization';
COMMENT ON COLUMN user_permissions.user_id IS 'Reference to user (UUID)';
COMMENT ON COLUMN user_permissions.authorities IS 'Direct authorities as JSON array string (e.g., ["MEMBERS:READ", "TRAINING:VIEW"])';
COMMENT ON COLUMN user_permissions.created_at IS 'Timestamp when permissions were created';
COMMENT ON COLUMN user_permissions.modified_at IS 'Timestamp when permissions were last modified';

-- ============================================================================
-- 4. PASSWORD_SETUP_TOKENS TABLE
-- Tokens for user self-service password setup during account activation
-- ============================================================================

CREATE TABLE password_setup_tokens
(
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP   NOT NULL,
    used_at    TIMESTAMP,
    used_by_ip VARCHAR(45),
    CONSTRAINT unique_user_token UNIQUE (user_id, created_at)
);

-- Indexes for password_setup_tokens
CREATE INDEX idx_password_tokens_user_id ON password_setup_tokens (user_id);
CREATE INDEX idx_password_tokens_token_hash ON password_setup_tokens (token_hash);
CREATE INDEX idx_password_tokens_expires_at ON password_setup_tokens (expires_at);
CREATE INDEX idx_password_tokens_created_at ON password_setup_tokens (created_at);

-- Comments for password_setup_tokens
COMMENT ON TABLE password_setup_tokens IS 'Tokens for user self-service password setup during account activation';
COMMENT ON COLUMN password_setup_tokens.id IS 'Unique token identifier (UUID)';
COMMENT ON COLUMN password_setup_tokens.user_id IS 'Reference to user account requiring password setup';
COMMENT ON COLUMN password_setup_tokens.token_hash IS 'SHA-256 hash of the random token (never store plain text)';
COMMENT ON COLUMN password_setup_tokens.created_at IS 'Token generation timestamp';
COMMENT ON COLUMN password_setup_tokens.expires_at IS 'Token expiration timestamp (typically 4 hours after generation)';
COMMENT ON COLUMN password_setup_tokens.used_at IS 'Timestamp when token was used to set password (null if unused)';
COMMENT ON COLUMN password_setup_tokens.used_by_ip IS 'IP address of user who set password (for security audit)';

-- ============================================================================
-- 5. OAUTH2_REGISTERED_CLIENT TABLE
-- OAuth2 client registrations (Spring Authorization Server schema)
-- ============================================================================

CREATE TABLE oauth2_registered_client
(
    id                            VARCHAR(100)  NOT NULL PRIMARY KEY,
    client_id                     VARCHAR(100)  NOT NULL UNIQUE,
    client_id_issued_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    client_secret                 VARCHAR(200),
    client_secret_expires_at      TIMESTAMP,
    client_name                   VARCHAR(200)  NOT NULL,
    client_authentication_methods VARCHAR(1000) NOT NULL,
    authorization_grant_types     VARCHAR(1000) NOT NULL,
    redirect_uris                 VARCHAR(1000),
    post_logout_redirect_uris     VARCHAR(1000),
    scopes                        VARCHAR(1000) NOT NULL,
    client_settings               VARCHAR(2000) NOT NULL,
    token_settings                VARCHAR(2000) NOT NULL
);

-- Comments for oauth2_registered_client
COMMENT ON TABLE oauth2_registered_client IS 'OAuth2 client registrations';
COMMENT ON COLUMN oauth2_registered_client.client_secret IS 'BCrypt-hashed client secret';

-- ============================================================================
-- 6. OAUTH2_AUTHORIZATION TABLE
-- OAuth2 authorization records (Spring Authorization Server schema)
-- ============================================================================

CREATE TABLE oauth2_authorization
(
    id                            varchar(100) NOT NULL,
    registered_client_id          varchar(100) NOT NULL,
    principal_name                varchar(200) NOT NULL,
    authorization_grant_type      varchar(100) NOT NULL,
    authorized_scopes             varchar(1000) DEFAULT NULL,
    attributes                    text          DEFAULT NULL,
    state                         varchar(500)  DEFAULT NULL,
    authorization_code_value      text          DEFAULT NULL,
    authorization_code_issued_at  timestamp     DEFAULT NULL,
    authorization_code_expires_at timestamp     DEFAULT NULL,
    authorization_code_metadata   text          DEFAULT NULL,
    access_token_value            text          DEFAULT NULL,
    access_token_issued_at        timestamp     DEFAULT NULL,
    access_token_expires_at       timestamp     DEFAULT NULL,
    access_token_metadata         text          DEFAULT NULL,
    access_token_type             varchar(100)  DEFAULT NULL,
    access_token_scopes           varchar(1000) DEFAULT NULL,
    oidc_id_token_value           text          DEFAULT NULL,
    oidc_id_token_issued_at       timestamp     DEFAULT NULL,
    oidc_id_token_expires_at      timestamp     DEFAULT NULL,
    oidc_id_token_metadata        text          DEFAULT NULL,
    refresh_token_value           text          DEFAULT NULL,
    refresh_token_issued_at       timestamp     DEFAULT NULL,
    refresh_token_expires_at      timestamp     DEFAULT NULL,
    refresh_token_metadata        text          DEFAULT NULL,
    user_code_value               text          DEFAULT NULL,
    user_code_issued_at           timestamp     DEFAULT NULL,
    user_code_expires_at          timestamp     DEFAULT NULL,
    user_code_metadata            text          DEFAULT NULL,
    device_code_value             text          DEFAULT NULL,
    device_code_issued_at         timestamp     DEFAULT NULL,
    device_code_expires_at        timestamp     DEFAULT NULL,
    device_code_metadata          text          DEFAULT NULL,
    PRIMARY KEY (id)
);

-- Indexes for oauth2_authorization
CREATE INDEX idx_oauth2_authorization_registered_client_id ON oauth2_authorization (registered_client_id);
CREATE INDEX idx_oauth2_authorization_principal_name ON oauth2_authorization (principal_name);
CREATE INDEX idx_oauth2_authorization_access_token_expires_at ON oauth2_authorization (access_token_expires_at);
CREATE INDEX idx_oauth2_authorization_refresh_token_expires_at ON oauth2_authorization (refresh_token_expires_at);

-- Comments for oauth2_authorization
COMMENT ON TABLE oauth2_authorization IS 'OAuth2 authorization records';

-- ============================================================================
-- 7. OAUTH2_AUTHORIZATION_CONSENT TABLE
-- OAuth2 authorization consent records (Spring Authorization Server schema)
-- ============================================================================

CREATE TABLE oauth2_authorization_consent
(
    registered_client_id VARCHAR(100)  NOT NULL,
    principal_name       VARCHAR(500)  NOT NULL,
    authorities          VARCHAR(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);

-- ============================================================================
-- 8. EVENT_PUBLICATION TABLE
-- Spring Modulith Event Publication Outbox Table
-- Stores domain events atomically with aggregate state changes
-- Events are persisted in the same transaction as the aggregate, then published asynchronously
-- Provides guaranteed at-least-once delivery with automatic retry
-- ============================================================================

CREATE TABLE event_publication
(
    -- Unique identifier for each event publication attempt
    id               UUID          NOT NULL PRIMARY KEY,

    -- Fully qualified class name of the event type
    -- Example: com.klabis.members.domain.events.MemberCreatedEvent
    event_type       VARCHAR(512)  NOT NULL,

    -- Identifier of the event listener/method
    -- Example: com.klabis.members.registration.MemberCreatedEventHandler.onMemberCreated
    listener_id      VARCHAR(512)  NOT NULL,

    -- When the event was first persisted to the outbox
    publication_date TIMESTAMP     NOT NULL,

    -- Serialized event object (JSON format)
    -- Contains all event data needed for processing
    -- Using VARCHAR(4000) which works for both H2 and PostgreSQL (from V010)
    -- PostgreSQL TEXT would be ideal but H2 interprets TEXT as VARCHAR(255)
    serialized_event VARCHAR(4000) NOT NULL,

    -- When the event was successfully processed (null if not yet completed)
    -- Allows tracking incomplete events for retry
    completion_date  TIMESTAMP
);

-- Indexes for event_publication
CREATE INDEX idx_event_publication_completion_date ON event_publication (completion_date);
CREATE INDEX idx_event_publication_publication_date ON event_publication (publication_date);

-- ============================================================================
-- 9. EVENTS TABLE
-- Stores orienteering event information
-- ============================================================================

CREATE TABLE events
(
    id                   UUID PRIMARY KEY,
    name                 VARCHAR(200) NOT NULL,
    event_date           DATE         NOT NULL,
    location             VARCHAR(200) NOT NULL,
    organizer            VARCHAR(10)  NOT NULL,
    website_url          VARCHAR(500) NULL,
    event_coordinator_id UUID         NULL REFERENCES members (id) ON DELETE SET NULL,
    status               VARCHAR(20)  NOT NULL,

    -- Audit fields
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by           VARCHAR(100) NOT NULL,
    modified_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by          VARCHAR(100) NOT NULL,
    version              BIGINT       NOT NULL DEFAULT 0
);

-- Indexes for events
CREATE INDEX idx_events_status ON events (status);
CREATE INDEX idx_events_event_date ON events (event_date);
CREATE INDEX idx_events_organizer ON events (organizer);

-- Comments for events
COMMENT ON TABLE events IS 'Stores orienteering event information';
COMMENT ON COLUMN events.id IS 'Unique event identifier (UUID)';
COMMENT ON COLUMN events.name IS 'Name of the event';
COMMENT ON COLUMN events.event_date IS 'Date when the event takes place';
COMMENT ON COLUMN events.location IS 'Location description (city, venue, etc.)';
COMMENT ON COLUMN events.organizer IS 'Organizer code (e.g., OOB for OOB Zdar nad Sazavou)';
COMMENT ON COLUMN events.website_url IS 'Optional URL to event website or ORIS';
COMMENT ON COLUMN events.event_coordinator_id IS 'Optional reference to club member coordinating the event';
COMMENT ON COLUMN events.status IS 'Event status (e.g., DRAFT, PUBLISHED, CANCELLED, COMPLETED)';
COMMENT ON COLUMN events.created_at IS 'Timestamp when event was created';
COMMENT ON COLUMN events.created_by IS 'User who created the event';
COMMENT ON COLUMN events.modified_at IS 'Timestamp when event was last modified';
COMMENT ON COLUMN events.modified_by IS 'User who last modified the event';
COMMENT ON COLUMN events.version IS 'Optimistic locking version';

-- ============================================================================
-- 10. EVENT_REGISTRATIONS TABLE
-- Stores member registrations for events
-- ============================================================================

CREATE TABLE event_registrations
(
    id             UUID PRIMARY KEY,
    event_id       UUID       NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    member_id      UUID       NOT NULL REFERENCES members (id) ON DELETE CASCADE,
    si_card_number VARCHAR(8) NOT NULL,
    registered_at  TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Unique constraint: one registration per member per event
    CONSTRAINT uk_event_registrations_event_member UNIQUE (event_id, member_id)
);

-- Indexes for event_registrations
CREATE INDEX idx_event_registrations_event_id ON event_registrations (event_id);

-- Comments for event_registrations
COMMENT ON TABLE event_registrations IS 'Stores member registrations for events';
COMMENT ON COLUMN event_registrations.id IS 'Unique registration identifier (UUID)';
COMMENT ON COLUMN event_registrations.event_id IS 'Reference to the event';
COMMENT ON COLUMN event_registrations.member_id IS 'Reference to the registered member';
COMMENT ON COLUMN event_registrations.si_card_number IS 'SI (SportIdent) card number used for the event';
COMMENT ON COLUMN event_registrations.registered_at IS 'Timestamp when member registered for the event';

-- ============================================================================
-- BOOTSTRAP DATA NOTE
-- Bootstrap data (admin user and OAuth2 client) is managed by
-- BootstrapDataLoader component which reads credentials from environment variables.
-- This prevents credentials from being exposed in version control.
-- See: com.klabis.config.BootstrapDataLoader
-- ============================================================================
