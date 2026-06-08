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
-- 4a. event_types (no dependencies — must precede events)
-- 5. events (FK → members, event_types)
-- 6. event_registrations (FK → events, members)
-- 7. calendar_items (FK → events)
-- 8. birth_number_audit_log (no FK)
-- 9. user_groups (unified table: type discriminator FREE/TRAINING/FAMILY)
-- 10. user_group_owners + user_group_members + user_group_invitations (FK → user_groups)
--
-- OAuth2 infrastructure tables created in V002 migration
-- Spring Modulith infrastructure tables created in V003 migration

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
    trainer_license_level VARCHAR(10),
    trainer_license_validity_date DATE,
    referee_license_level VARCHAR(10),
    referee_license_validity_date DATE,
    driving_license_group VARCHAR(10),
    dietary_restrictions  VARCHAR(500),

    -- Birth number and bank account (GDPR sensitive fields)
    birth_number          VARCHAR(255),
    bank_account_number   VARCHAR(50),

    -- Suspension fields
    suspension_reason     VARCHAR(20),
    suspended_at          TIMESTAMP,
    suspension_note       VARCHAR(500),
    suspended_by          VARCHAR(100),

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
COMMENT ON COLUMN members.trainer_license_level IS 'Trainer license level (nullable, enum T1/T2/T3)';
COMMENT ON COLUMN members.trainer_license_validity_date IS 'Trainer license validity expiration date (nullable)';
COMMENT ON COLUMN members.referee_license_level IS 'Referee license level (nullable, enum R1/R2/R3)';
COMMENT ON COLUMN members.referee_license_validity_date IS 'Referee license validity expiration date (nullable)';
COMMENT ON COLUMN members.driving_license_group IS 'Driving license group/category (nullable, max 10 characters, e.g., BE, C1, C1E)';
COMMENT ON COLUMN members.dietary_restrictions IS 'Dietary restrictions or food allergies (nullable, max 500 characters)';
COMMENT ON COLUMN members.birth_number IS 'Czech birth number (rodné číslo), encrypted with Jasypt, format RRMMDD/XXXX (nullable, only for Czech nationals)';
COMMENT ON COLUMN members.bank_account_number IS 'Bank account number in IBAN or domestic Czech format for expense reimbursement (nullable)';

-- ============================================================================
-- 2. USERS TABLE
-- User accounts for authentication
-- ============================================================================

CREATE TABLE users
(
    id                      UUID PRIMARY KEY,
    user_name               VARCHAR(7)   NOT NULL UNIQUE,
    password_hash           VARCHAR(255) NOT NULL,
    account_status          VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',

    -- Audit fields (from V008)
    created_at              TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    modified_at             TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    version                 BIGINT       DEFAULT 0,

    -- Audit creator fields (from V011)
    created_by              VARCHAR(100),
    last_modified_by        VARCHAR(100)
);

-- Indexes for users
CREATE INDEX idx_users_user_name ON users (user_name);
CREATE INDEX idx_users_account_status ON users (account_status);
CREATE INDEX idx_users_created_at ON users (created_at);

-- Comments for users table
COMMENT ON TABLE users IS 'User accounts for authentication';
COMMENT ON COLUMN users.user_name IS 'Unique username for login. For members, auto-populated from registration number (XXXYYDD format). For standalone users, can be any unique string.';
COMMENT ON COLUMN users.password_hash IS 'BCrypt-hashed password';

-- ============================================================================
-- 3. USER_PERMISSIONS TABLE
-- User permissions/authorities, separated from User entity for authorization
-- Note: user_authorities table (V002) was deprecated and removed - authorization uses this table exclusively
-- ============================================================================

CREATE TABLE user_permissions
(
    user_id          UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    authorities      VARCHAR(1000) NOT NULL, -- JSON array of authority strings: ["MEMBERS:READ", "TRAINING:VIEW"]
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       VARCHAR(100),           -- User who created these permissions
    modified_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by VARCHAR(100),           -- User who last modified these permissions
    version          BIGINT        NOT NULL DEFAULT 0  -- Optimistic locking
);

-- Index for user_permissions
CREATE INDEX idx_user_permissions_user_id ON user_permissions (user_id);

-- Comments for user_permissions
COMMENT ON TABLE user_permissions IS 'User permissions/authorities, separated from User entity for authorization';
COMMENT ON COLUMN user_permissions.user_id IS 'Reference to user (UUID)';
COMMENT ON COLUMN user_permissions.authorities IS 'Direct authorities as JSON array string (e.g., ["MEMBERS:READ", "TRAINING:VIEW"])';
COMMENT ON COLUMN user_permissions.created_at IS 'Timestamp when permissions were created';
COMMENT ON COLUMN user_permissions.created_by IS 'User who created these permissions';
COMMENT ON COLUMN user_permissions.modified_at IS 'Timestamp when permissions were last modified';
COMMENT ON COLUMN user_permissions.last_modified_by IS 'User who last modified these permissions';

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
    created_by  VARCHAR(100),
    modified_at TIMESTAMP,
    modified_by VARCHAR(100),
    version     BIGINT      NOT NULL DEFAULT 0,
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
COMMENT ON COLUMN password_setup_tokens.created_by IS 'Identifier of who created this token (for audit trail)';
COMMENT ON COLUMN password_setup_tokens.modified_at IS 'Timestamp when token was last modified';
COMMENT ON COLUMN password_setup_tokens.modified_by IS 'Identifier of who last modified this token (for audit trail)';
COMMENT ON COLUMN password_setup_tokens.version IS 'Optimistic locking version for concurrent modification detection';

-- ============================================================================
-- 4a. EVENT_TYPES TABLE
-- Catalog of event types (e.g. Training, Race, Championship) managed by admin
-- Must be created before events table due to FK dependency
-- ============================================================================

CREATE TABLE event_types
(
    id          UUID         NOT NULL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    color       VARCHAR(7)   NULL,
    sort_order  INT          NOT NULL,

    -- Audit fields
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(100) NOT NULL,
    modified_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR(100) NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0
);

-- Indexes for event_types
-- Case-insensitive uniqueness is enforced at application layer (existsByNameIgnoreCase).
-- A functional index CREATE UNIQUE INDEX ON event_types (LOWER(name)) is the correct
-- PostgreSQL constraint, but H2 2.x does not support functional indexes — rejected by the
-- test suite. The plain index below enforces exact-case uniqueness at the DB level only;
-- the application-layer check covers the case-insensitive invariant.
CREATE UNIQUE INDEX idx_event_types_name ON event_types (name);
CREATE INDEX idx_event_types_sort_order ON event_types (sort_order);

-- Comments for event_types
COMMENT ON TABLE event_types IS 'Catalog of event types managed by admin (e.g. Training, Race, Championship)';
COMMENT ON COLUMN event_types.name IS 'Display name of the event type — unique case-insensitively';
COMMENT ON COLUMN event_types.color IS 'Optional hex color code for display (e.g. #ff0000)';
COMMENT ON COLUMN event_types.sort_order IS 'Position in sorted lists and filters; newly created types get MAX+1';

-- ============================================================================
-- 4b. EVENT_TYPE_ORIS_DISCIPLINES TABLE
-- Maps ORIS discipline IDs to event types (each discipline ID belongs to at most one event type)
-- ============================================================================

CREATE TABLE event_type_oris_disciplines
(
    event_type_id UUID NOT NULL REFERENCES event_types (id) ON DELETE CASCADE,
    discipline_id INT  NOT NULL
);

CREATE UNIQUE INDEX idx_event_type_oris_disciplines_discipline ON event_type_oris_disciplines (discipline_id);

-- ============================================================================
-- 5. EVENTS TABLE
-- Stores orienteering event information
-- ============================================================================

CREATE TABLE events
(
    id                   UUID PRIMARY KEY,
    name                 VARCHAR(200) NOT NULL,
    event_date           DATE         NOT NULL,
    location             VARCHAR(200) NULL,
    organizer            VARCHAR(10)  NOT NULL,
    website_url          VARCHAR(500) NULL,
    event_coordinator_id UUID         NULL REFERENCES members (id) ON DELETE SET NULL,
    status               VARCHAR(20)  NOT NULL,

    -- Registration deadlines: up to 3 sequential deadlines (d2 requires d1, d3 requires d2; all non-decreasing)
    registration_deadline  DATE         NULL,
    registration_deadline_2 DATE        NULL,
    registration_deadline_3 DATE        NULL,

    -- ORIS integration: source identifier for imported events (null for manually created events)
    oris_id              INTEGER      NULL UNIQUE,

    -- Race categories available at this event (comma-separated, e.g. "M21,W35,D10"; null means no categories defined)
    categories           VARCHAR(2000) NULL,

    -- Optional free-text reason provided when the event is cancelled (null when not cancelled or no reason given)
    cancellation_reason  VARCHAR(500)  NULL,

    -- Optional event type reference from the event-types catalog
    event_type_id        UUID         NULL REFERENCES event_types(id),

    -- Event ranking/level from ORIS (null for events without a defined competition level)
    level_id             INT          NULL,
    level_short_name     VARCHAR(20)  NULL,
    level_name           VARCHAR(100) NULL,

    -- Base entry fee (null for events without a defined entry fee)
    base_entry_fee_amount   DECIMAL(10, 2) NULL,
    base_entry_fee_currency CHAR(3)        NULL,

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
COMMENT ON COLUMN events.location IS 'Location description (city, venue, etc.) — nullable, not all events have a known location';
COMMENT ON COLUMN events.organizer IS 'Organizer code (e.g., OOB for OOB Zdar nad Sazavou)';
COMMENT ON COLUMN events.website_url IS 'Optional URL to event website or ORIS';
COMMENT ON COLUMN events.event_coordinator_id IS 'Optional reference to club member coordinating the event';
COMMENT ON COLUMN events.registration_deadline IS 'First (earliest) registration deadline; null means no deadlines configured';
COMMENT ON COLUMN events.registration_deadline_2 IS 'Second registration deadline; requires registration_deadline to be set';
COMMENT ON COLUMN events.registration_deadline_3 IS 'Third (latest) registration deadline; requires registration_deadline_2 to be set';
COMMENT ON COLUMN events.status IS 'Event status (e.g., DRAFT, PUBLISHED, CANCELLED, COMPLETED)';
COMMENT ON COLUMN events.created_at IS 'Timestamp when event was created';
COMMENT ON COLUMN events.created_by IS 'User who created the event';
COMMENT ON COLUMN events.modified_at IS 'Timestamp when event was last modified';
COMMENT ON COLUMN events.modified_by IS 'User who last modified the event';
COMMENT ON COLUMN events.version IS 'Optimistic locking version';

-- ============================================================================
-- 7. EVENT_REGISTRATIONS TABLE
-- Stores member registrations for events
-- ============================================================================

CREATE TABLE event_registrations
(
    id             UUID PRIMARY KEY,
    event_id       UUID       NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    member_id      UUID       NOT NULL REFERENCES members (id) ON DELETE CASCADE,
    si_card_number VARCHAR(8) NOT NULL,
    category       VARCHAR(50) NULL,
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
COMMENT ON COLUMN event_registrations.category IS 'Selected race category (nullable: null when event has no categories)';
COMMENT ON COLUMN event_registrations.registered_at IS 'Timestamp when member registered for the event';

-- ============================================================================
-- 8. CALENDAR_ITEMS TABLE
-- Stores calendar items (manual and event-linked)
-- ============================================================================

CREATE TABLE calendar_items
(
    id               UUID         PRIMARY KEY,
    kind             VARCHAR(32)  NOT NULL DEFAULT 'EVENT_DATE',
    name             VARCHAR(200) NOT NULL,
    description      TEXT         NULL,
    start_date       DATE         NOT NULL,
    end_date         DATE         NOT NULL,
    event_id         UUID         NULL, -- REFERENCES events (id) ON DELETE SET NULL,

    -- Audit fields
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       VARCHAR(100) NOT NULL,
    modified_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by VARCHAR(100) NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0
);

-- Indexes for calendar_items
CREATE INDEX idx_calendar_items_date_range ON calendar_items (start_date, end_date);
CREATE INDEX idx_calendar_items_event_id ON calendar_items (event_id);

-- Comments for calendar_items
COMMENT ON TABLE calendar_items IS 'Stores calendar items (manual and event-linked)';
COMMENT ON COLUMN calendar_items.id IS 'Unique calendar item identifier (UUID)';
COMMENT ON COLUMN calendar_items.kind IS 'Discriminator: MANUAL or EVENT_DATE (single-table inheritance)';
COMMENT ON COLUMN calendar_items.name IS 'Calendar item name (event name for linked items)';
COMMENT ON COLUMN calendar_items.description IS 'Calendar item description (location + organizer + website for linked items)';
COMMENT ON COLUMN calendar_items.start_date IS 'Start date of the calendar item';
COMMENT ON COLUMN calendar_items.end_date IS 'End date of the calendar item (same as start_date for single-day items)';
COMMENT ON COLUMN calendar_items.event_id IS 'Reference to linked event (NULL for manual items, ON DELETE SET NULL)';
COMMENT ON COLUMN calendar_items.created_at IS 'Timestamp when calendar item was created';
COMMENT ON COLUMN calendar_items.created_by IS 'User who created the calendar item';
COMMENT ON COLUMN calendar_items.modified_at IS 'Timestamp when calendar item was last modified';
COMMENT ON COLUMN calendar_items.last_modified_by IS 'User who last modified the calendar item';
COMMENT ON COLUMN calendar_items.version IS 'Optimistic locking version';

-- ============================================================================
-- 9. BIRTH_NUMBER_AUDIT_LOG TABLE
-- GDPR audit trail for birth number (rodné číslo) access and modification
-- ============================================================================

CREATE TABLE birth_number_audit_log
(
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL,
    member_id   UUID         NOT NULL,
    action      VARCHAR(30)  NOT NULL,
    occurred_at TIMESTAMP    NOT NULL
);

-- Indexes for birth_number_audit_log
CREATE INDEX idx_bn_audit_member_id ON birth_number_audit_log (member_id);
CREATE INDEX idx_bn_audit_user_id ON birth_number_audit_log (user_id);
CREATE INDEX idx_bn_audit_occurred_at ON birth_number_audit_log (occurred_at);

-- Comments for birth_number_audit_log
COMMENT ON TABLE birth_number_audit_log IS 'GDPR-compliant audit trail for birth number (rodné číslo) access and modification';
COMMENT ON COLUMN birth_number_audit_log.user_id IS 'ID of the user who accessed or modified the birth number';
COMMENT ON COLUMN birth_number_audit_log.member_id IS 'ID of the member whose birth number was accessed or modified (no FK – audit records are retained after member deletion)';
COMMENT ON COLUMN birth_number_audit_log.action IS 'Action type: VIEW_BIRTH_NUMBER or MODIFY_BIRTH_NUMBER';
COMMENT ON COLUMN birth_number_audit_log.occurred_at IS 'Timestamp when the action occurred';

-- ============================================================================
-- 14. USER_GROUPS TABLE
-- Unified table for all group types (FREE = FreeGroup, TRAINING = TrainingGroup, FAMILY = FamilyGroup)
-- The type column acts as a discriminator to separate groups by their aggregate type.
-- age_range_min/max are used only by TRAINING groups; NULL for FREE and FAMILY groups.
-- ============================================================================

CREATE TABLE user_groups
(
    id            UUID         NOT NULL PRIMARY KEY,
    type          VARCHAR(20)  NOT NULL,
    name          VARCHAR(200) NOT NULL,
    age_range_min INT          NULL,
    age_range_max INT          NULL,

    -- Audit fields
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(100) NOT NULL,
    modified_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by   VARCHAR(100) NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT chk_user_groups_type CHECK (type IN ('FREE', 'TRAINING', 'FAMILY'))
);

-- Indexes for user_groups
CREATE INDEX idx_user_groups_type ON user_groups (type);
CREATE INDEX idx_user_groups_name ON user_groups (name);
CREATE INDEX idx_user_groups_training_age ON user_groups (type, age_range_min, age_range_max);

-- Comments for user_groups
COMMENT ON TABLE user_groups IS 'Unified table for all group aggregate types: FREE (FreeGroup), TRAINING (TrainingGroup), FAMILY (FamilyGroup)';
COMMENT ON COLUMN user_groups.type IS 'Discriminator: FREE = invitation-based members group, TRAINING = age-range training group, FAMILY = family group';
COMMENT ON COLUMN user_groups.age_range_min IS 'Minimum age (inclusive) — populated only for TRAINING groups';
COMMENT ON COLUMN user_groups.age_range_max IS 'Maximum age (inclusive) — populated only for TRAINING groups';

-- ============================================================================
-- 15. USER_GROUP_OWNERS TABLE
-- Maps owners/trainers/parents to their group (role depends on the group type).
-- ============================================================================

CREATE TABLE user_group_owners
(
    user_group_id UUID NOT NULL REFERENCES user_groups (id) ON DELETE CASCADE,
    member_id     UUID NOT NULL,
    PRIMARY KEY (user_group_id, member_id)
);

-- Indexes for user_group_owners
CREATE INDEX idx_user_group_owners_group_id ON user_group_owners (user_group_id);
CREATE INDEX idx_user_group_owners_member_id ON user_group_owners (member_id);

-- Comments for user_group_owners
COMMENT ON TABLE user_group_owners IS 'Owners/trainers/parents per group — interpretation depends on the group type';

-- ============================================================================
-- 16. USER_GROUP_MEMBERS TABLE
-- Maps members to groups with join timestamp, shared across all group types.
-- ============================================================================

CREATE TABLE user_group_members
(
    user_group_id UUID      NOT NULL REFERENCES user_groups (id) ON DELETE CASCADE,
    member_id     UUID      NOT NULL,
    joined_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_group_id, member_id)
);

-- Indexes for user_group_members
CREATE INDEX idx_user_group_members_group_id ON user_group_members (user_group_id);
CREATE INDEX idx_user_group_members_member_id ON user_group_members (member_id);

-- Comments for user_group_members
COMMENT ON TABLE user_group_members IS 'Members of any group type with join timestamp';
COMMENT ON COLUMN user_group_members.joined_at IS 'Timestamp when the member was added to the group';

-- ============================================================================
-- 17. USER_GROUP_INVITATIONS TABLE
-- Membership invitations — used only by FREE (FreeGroup) groups.
-- ============================================================================

CREATE TABLE user_group_invitations
(
    id                   UUID        NOT NULL PRIMARY KEY,
    user_group_id        UUID        NOT NULL REFERENCES user_groups (id) ON DELETE CASCADE,
    invited_member_id    UUID        NOT NULL,
    invited_by_member_id UUID        NOT NULL,
    status               VARCHAR(20) NOT NULL,
    created_at           TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at         TIMESTAMP   NULL,
    cancelled_by         UUID        NULL,
    cancellation_reason  VARCHAR(500) NULL
);

-- Indexes for user_group_invitations
CREATE INDEX idx_user_group_invitations_group_id ON user_group_invitations (user_group_id);
CREATE INDEX idx_user_group_invitations_invited_member_status ON user_group_invitations (invited_member_id, status);

-- Comments for user_group_invitations
COMMENT ON TABLE user_group_invitations IS 'Membership invitations for FREE (FreeGroup) groups';
COMMENT ON COLUMN user_group_invitations.status IS 'Invitation status: PENDING, ACCEPTED, REJECTED, CANCELLED';
COMMENT ON COLUMN user_group_invitations.cancelled_at IS 'Timestamp when the invitation was cancelled';
COMMENT ON COLUMN user_group_invitations.cancelled_by IS 'MemberId of the member who cancelled the invitation';
COMMENT ON COLUMN user_group_invitations.cancellation_reason IS 'Optional free-text reason for cancellation';

-- ============================================================================
-- 24. CATEGORY_PRESETS TABLE
-- Reusable category preset templates for events
-- ============================================================================

CREATE TABLE category_presets
(
    id          UUID         NOT NULL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    categories  VARCHAR(2000) NULL,

    -- Audit fields
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(100) NOT NULL,
    modified_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR(100) NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0
);

-- Indexes for category_presets
CREATE INDEX idx_category_presets_name ON category_presets (name);

-- Comments for category_presets
COMMENT ON TABLE category_presets IS 'Reusable category preset templates for events (copy-on-apply, no link maintained)';
COMMENT ON COLUMN category_presets.categories IS 'Comma-separated category names (e.g. "M21,W35,D10"; null means empty preset)';

-- ============================================================================
-- 25. CALENDAR_FEED_TOKEN TABLE
-- Per-user personal access token for iCalendar feed authentication.
-- Owned by the calendar module — User aggregate in common.users is not modified.
-- ============================================================================

CREATE TABLE calendar_feed_token
(
    user_id     UUID         NOT NULL PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL,
    token_lookup VARCHAR(16) NOT NULL,
    last_set_at TIMESTAMP    NOT NULL
);

-- Indexed lookup: find token row by non-secret prefix before bcrypt comparison (O(1), no full-table scan)
CREATE INDEX idx_calendar_feed_token_lookup ON calendar_feed_token (token_lookup);

-- Comments for calendar_feed_token
COMMENT ON TABLE calendar_feed_token IS 'Per-user PAT for iCalendar feed. One row per user (upsert on regenerate). Owned by the calendar module.';
COMMENT ON COLUMN calendar_feed_token.user_id IS 'Owner user ID — primary key (single token per user)';
COMMENT ON COLUMN calendar_feed_token.token_hash IS 'BCrypt/Argon2 hash of the raw token (never stored plain)';
COMMENT ON COLUMN calendar_feed_token.token_lookup IS 'Non-secret 8-char prefix of the raw base64url token, used for indexed pre-filter before bcrypt comparison';
COMMENT ON COLUMN calendar_feed_token.last_set_at IS 'When the token was last generated or regenerated';

-- ============================================================================
-- 26. MEMBER_ACCOUNT TABLE
-- Financial account for each club member (1:1 with members).
-- Account is created automatically when a member is registered.
-- Owned by the finance module — members table is not modified.
-- ============================================================================

CREATE TABLE member_account
(
    member_id         UUID           NOT NULL PRIMARY KEY,
    balance_amount    DECIMAL(19, 4) NOT NULL DEFAULT 0,
    balance_currency  CHAR(3)        NOT NULL DEFAULT 'CZK',
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version           BIGINT         NOT NULL DEFAULT 0
);

COMMENT ON TABLE member_account IS 'Financial prepaid account per club member (1:1). Owned by the finance module.';
COMMENT ON COLUMN member_account.member_id IS 'Primary key — equals members.id (shared identity, no FK constraint across module boundary)';
COMMENT ON COLUMN member_account.balance_amount IS 'Current cached balance (sum of all transaction amounts); positive = credit, negative = debt';
COMMENT ON COLUMN member_account.balance_currency IS 'ISO 4217 currency code (v1: CZK only)';

-- ============================================================================
-- 27. FINANCE_TRANSACTION TABLE
-- Append-only ledger of transactions for each member's financial account.
-- Owned by the finance module.
-- ============================================================================

CREATE TABLE finance_transaction
(
    id                      UUID           NOT NULL PRIMARY KEY,
    member_account_id       UUID           NOT NULL REFERENCES member_account (member_id),
    type                    VARCHAR(20)    NOT NULL,
    amount                  DECIMAL(19, 4) NOT NULL,
    currency                CHAR(3)        NOT NULL DEFAULT 'CZK',
    note                    TEXT,
    recorded_at             TIMESTAMP      NOT NULL,
    occurred_at             DATE           NOT NULL,
    recorded_by_user_id     UUID           NOT NULL,
    reverses_transaction_id UUID           NULL
);

-- Lookup index: history ordered by occurred_at DESC
CREATE INDEX idx_finance_transaction_account_occurred ON finance_transaction (member_account_id, occurred_at DESC);

-- Prevents the same transaction from being reversed twice.
-- H2 treats multiple NULLs as distinct in UNIQUE constraints (unlike some other databases),
-- so a plain UNIQUE index works for both H2 (dev/test) and PostgreSQL (production).
-- Domain layer enforces this invariant as well (TransactionAlreadyReversedException).
CREATE UNIQUE INDEX idx_finance_transaction_reverses_unique ON finance_transaction (reverses_transaction_id);

COMMENT ON TABLE finance_transaction IS 'Append-only ledger of financial transactions per member account. Owned by the finance module.';
COMMENT ON COLUMN finance_transaction.type IS 'Transaction type: DEPOSIT (positive, credit) or OTHER (negative, debit)';
COMMENT ON COLUMN finance_transaction.amount IS 'Signed amount: DEPOSIT > 0, OTHER < 0';
COMMENT ON COLUMN finance_transaction.recorded_at IS 'Server timestamp when the entry was recorded (audit, immutable)';
COMMENT ON COLUMN finance_transaction.occurred_at IS 'Date when the underlying financial event occurred (entered by finance manager)';
COMMENT ON COLUMN finance_transaction.reverses_transaction_id IS 'If set, this transaction is a reversal of the referenced transaction';

-- ============================================================================
-- 28. MEMBERSHIP_FEE_LEVEL TABLE
-- Catalog of membership fee level templates (MembershipFeeLevel aggregate root).
-- Editable at any time; published levels keep a snapshot (membership_fee_group).
-- ============================================================================

CREATE TABLE membership_fee_level
(
    id                    UUID           NOT NULL PRIMARY KEY,
    name                  VARCHAR(200)   NOT NULL,
    yearly_fee_amount     DECIMAL(19, 4) NOT NULL,
    yearly_fee_currency   VARCHAR(3)     NOT NULL DEFAULT 'CZK',
    created_at            TIMESTAMP      NULL,
    created_by            VARCHAR(255)   NULL,
    modified_at           TIMESTAMP      NULL,
    modified_by           VARCHAR(255)   NULL,
    version               BIGINT         NOT NULL DEFAULT 0
);

-- Comments for membership_fee_level
COMMENT ON TABLE membership_fee_level IS 'Catalog of membership fee level templates. Editable; published levels snapshot this into membership_fee_group.';
COMMENT ON COLUMN membership_fee_level.yearly_fee_amount IS 'Annual membership fee for this level';
COMMENT ON COLUMN membership_fee_level.yearly_fee_currency IS 'ISO 4217 currency code (default CZK)';

-- ============================================================================
-- 29. MEMBERSHIP_PAYMENT_RULE TABLE
-- Co-payment rules owned by a MembershipFeeLevel (value objects).
-- Key: (membership_fee_level_id, event_type_id, ranking_short_name) — unique per level.
-- ============================================================================

CREATE TABLE membership_payment_rule
(
    id                       UUID        NOT NULL PRIMARY KEY,
    membership_fee_level_id  UUID        NOT NULL REFERENCES membership_fee_level (id) ON DELETE CASCADE,
    event_type_id            UUID        NOT NULL,
    ranking_short_name       VARCHAR(100) NOT NULL,
    rule_type                VARCHAR(20) NOT NULL,
    rule_percentage          INT         NULL,
    rule_fixed_amount        DECIMAL(19, 4) NULL,
    rule_fixed_currency      VARCHAR(3)  NULL,

    CONSTRAINT uk_membership_payment_rule UNIQUE (membership_fee_level_id, event_type_id, ranking_short_name),
    CONSTRAINT chk_membership_payment_rule_type CHECK (rule_type IN ('PERCENTAGE', 'FIXED_SURCHARGE'))
);

-- Indexes for membership_payment_rule
CREATE INDEX idx_membership_payment_rule_level ON membership_payment_rule (membership_fee_level_id);

-- Comments for membership_payment_rule
COMMENT ON TABLE membership_payment_rule IS 'Co-payment rules per (event_type, ranking) combination within a fee level template';
COMMENT ON COLUMN membership_payment_rule.event_type_id IS 'Reference to EventType aggregate (no FK — cross-module value object reference)';
COMMENT ON COLUMN membership_payment_rule.ranking_short_name IS 'Ranking short name as natural key (e.g. A, B, LOB); no FK until ranking table exists';
COMMENT ON COLUMN membership_payment_rule.rule_type IS 'PERCENTAGE: percent of base entry fee; FIXED_SURCHARGE: fixed amount added to entry fee';
COMMENT ON COLUMN membership_payment_rule.rule_percentage IS 'Percentage value (0-100+) — populated only when rule_type = PERCENTAGE';
COMMENT ON COLUMN membership_payment_rule.rule_fixed_amount IS 'Fixed surcharge amount — populated only when rule_type = FIXED_SURCHARGE';
COMMENT ON COLUMN membership_payment_rule.rule_fixed_currency IS 'Currency for fixed surcharge — populated only when rule_type = FIXED_SURCHARGE';

-- ============================================================================
-- 30. FEE_YEAR_PUBLICATION TABLE
-- Published set of fee levels for a calendar year (FeeYearPublication aggregate root).
-- One publication per year; owns the voting deadline for all levels in that year.
-- ============================================================================

CREATE TABLE fee_year_publication
(
    id                      UUID    NOT NULL PRIMARY KEY,
    publication_year        INT     NOT NULL,
    voting_deadline         DATE    NOT NULL,
    deadline_processed_at   TIMESTAMP NULL,
    created_at              TIMESTAMP NULL,
    created_by              VARCHAR(255) NULL,
    modified_at             TIMESTAMP NULL,
    modified_by             VARCHAR(255) NULL,
    version                 BIGINT  NOT NULL DEFAULT 0,

    CONSTRAINT uk_fee_year_publication_year UNIQUE (publication_year)
);

-- Comments for fee_year_publication
COMMENT ON TABLE fee_year_publication IS 'Published fee level set for a calendar year with a single voting deadline';
COMMENT ON COLUMN fee_year_publication.publication_year IS 'Calendar year this publication applies to (unique)';
COMMENT ON COLUMN fee_year_publication.voting_deadline IS 'Deadline by which members must choose their level';
COMMENT ON COLUMN fee_year_publication.deadline_processed_at IS 'Timestamp when the post-deadline scheduler ran (null until processed)';

-- ============================================================================
-- 31. FEE_YEAR_PUBLICATION_LEVEL TABLE
-- Join table: FeeYearPublication → MembershipFeeGroup references.
-- ============================================================================

CREATE TABLE fee_year_publication_level
(
    fee_year_publication_id UUID NOT NULL REFERENCES fee_year_publication (id) ON DELETE CASCADE,
    membership_fee_group_id UUID NOT NULL,
    PRIMARY KEY (fee_year_publication_id, membership_fee_group_id)
);

-- Comments for fee_year_publication_level
COMMENT ON TABLE fee_year_publication_level IS 'Links a FeeYearPublication to its published MembershipFeeGroup instances';
COMMENT ON COLUMN fee_year_publication_level.membership_fee_group_id IS 'Reference to MembershipFeeGroup (no FK — same-module reference managed by aggregate)';

-- ============================================================================
-- 32. MEMBERSHIP_FEE_GROUP TABLE
-- Published fee level for one year (MembershipFeeGroup aggregate root).
-- Holds a snapshot of the level template at publication time plus membership records.
-- ============================================================================

CREATE TABLE membership_fee_group
(
    id                              UUID           NOT NULL PRIMARY KEY,
    source_level_id                 UUID           NOT NULL REFERENCES membership_fee_level (id),
    name                            VARCHAR(200)   NOT NULL,
    group_year                      INT            NOT NULL,
    yearly_fee_snapshot_amount      DECIMAL(19, 4) NOT NULL,
    yearly_fee_snapshot_currency    VARCHAR(3)     NOT NULL DEFAULT 'CZK',
    status                          VARCHAR(20)    NOT NULL DEFAULT 'EDITABLE',
    created_at                      TIMESTAMP NULL,
    created_by                      VARCHAR(255) NULL,
    modified_at                     TIMESTAMP NULL,
    modified_by                     VARCHAR(255) NULL,
    version                         BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT chk_membership_fee_group_status CHECK (status IN ('EDITABLE', 'FROZEN'))
);

-- Indexes for membership_fee_group
CREATE INDEX idx_membership_fee_group_year ON membership_fee_group (group_year);
CREATE INDEX idx_membership_fee_group_source_level ON membership_fee_group (source_level_id);

-- Comments for membership_fee_group
COMMENT ON TABLE membership_fee_group IS 'Snapshot of a fee level for a specific year; contains membership records for member choices';
COMMENT ON COLUMN membership_fee_group.source_level_id IS 'Template level this snapshot was created from (for D8: previous-year default lookup)';
COMMENT ON COLUMN membership_fee_group.name IS 'Snapshot of the level name at publication time';
COMMENT ON COLUMN membership_fee_group.group_year IS 'Calendar year this group applies to';
COMMENT ON COLUMN membership_fee_group.yearly_fee_snapshot_amount IS 'Snapshot of the yearly fee at publication time; immutable after FROZEN';
COMMENT ON COLUMN membership_fee_group.status IS 'EDITABLE: snapshot can be changed before voting deadline; FROZEN: locked after deadline';

-- ============================================================================
-- 33. MEMBERSHIP_FEE_GROUP_RULE_SNAPSHOT TABLE
-- Snapshot of co-payment rules for a published MembershipFeeGroup (MembershipPaymentRuleSnapshot value objects).
-- ============================================================================

CREATE TABLE membership_fee_group_rule_snapshot
(
    id                      UUID        NOT NULL PRIMARY KEY,
    membership_fee_group_id UUID        NOT NULL REFERENCES membership_fee_group (id) ON DELETE CASCADE,
    event_type_id           UUID        NOT NULL,
    ranking_short_name      VARCHAR(100) NOT NULL,
    rule_type               VARCHAR(20) NOT NULL,
    rule_percentage         INT         NULL,
    rule_fixed_amount       DECIMAL(19, 4) NULL,
    rule_fixed_currency     VARCHAR(3)  NULL,

    CONSTRAINT uk_membership_fee_group_rule_snapshot UNIQUE (membership_fee_group_id, event_type_id, ranking_short_name),
    CONSTRAINT chk_membership_fee_group_rule_snapshot_type CHECK (rule_type IN ('PERCENTAGE', 'FIXED_SURCHARGE'))
);

-- Indexes for membership_fee_group_rule_snapshot
CREATE INDEX idx_membership_fee_group_rule_snapshot_group ON membership_fee_group_rule_snapshot (membership_fee_group_id);

-- Comments for membership_fee_group_rule_snapshot
COMMENT ON TABLE membership_fee_group_rule_snapshot IS 'Snapshot of co-payment rules copied from the level template at publication time; independent of catalog changes';
COMMENT ON COLUMN membership_fee_group_rule_snapshot.event_type_id IS 'Reference to EventType (no FK — cross-module value object reference)';
COMMENT ON COLUMN membership_fee_group_rule_snapshot.ranking_short_name IS 'Ranking short name as natural key; no FK until ranking table exists (D3)';

-- ============================================================================
-- 34. FEE_GROUP_MEMBERSHIP TABLE
-- Member's choice or admin assignment to a MembershipFeeGroup for a year (FeeGroupMembership value objects).
-- ============================================================================

CREATE TABLE fee_group_membership
(
    id                      UUID        NOT NULL PRIMARY KEY,
    membership_fee_group_id UUID        NOT NULL REFERENCES membership_fee_group (id) ON DELETE CASCADE,
    member_id               UUID        NOT NULL,
    joined_at               DATE        NOT NULL,
    assignment_source       VARCHAR(20) NOT NULL,
    assigned_by             UUID        NULL,

    CONSTRAINT uk_fee_group_membership UNIQUE (membership_fee_group_id, member_id),
    CONSTRAINT chk_fee_group_membership_source CHECK (assignment_source IN ('MEMBER_CHOICE', 'ADMIN_ASSIGNMENT'))
);

-- Indexes for fee_group_membership
CREATE INDEX idx_fee_group_membership_group ON fee_group_membership (membership_fee_group_id);
CREATE INDEX idx_fee_group_membership_member ON fee_group_membership (member_id);

-- Comments for fee_group_membership
COMMENT ON TABLE fee_group_membership IS 'Records which members chose (or were assigned to) a fee level for a given year';
COMMENT ON COLUMN fee_group_membership.member_id IS 'Reference to Member aggregate (no FK — cross-module value object reference)';
COMMENT ON COLUMN fee_group_membership.joined_at IS 'Date the member was added to this fee group';
COMMENT ON COLUMN fee_group_membership.assignment_source IS 'MEMBER_CHOICE: member self-selected; ADMIN_ASSIGNMENT: assigned by admin (e.g. emergency assignment after deadline)';
COMMENT ON COLUMN fee_group_membership.assigned_by IS 'MemberId of the admin who performed an ADMIN_ASSIGNMENT; null for MEMBER_CHOICE';

-- ============================================================================
-- 35. YEARLY_FEE_CHARGE_MARKER TABLE
-- Idempotency marker preventing double-charging of yearly membership fee (D6).
-- Primary key on (member_id, year) ensures at-most-once per member per year.
-- ============================================================================

CREATE TABLE yearly_fee_charge_marker
(
    member_id    UUID      NOT NULL,
    charge_year  INT       NOT NULL,
    charged_at   TIMESTAMP NOT NULL,
    PRIMARY KEY (member_id, charge_year)
);

-- Comments for yearly_fee_charge_marker
COMMENT ON TABLE yearly_fee_charge_marker IS 'Idempotency guard: prevents double-posting the yearly membership fee for a (member, year) pair (D6)';
COMMENT ON COLUMN yearly_fee_charge_marker.member_id IS 'Reference to Member aggregate (no FK — cross-module value object reference)';
COMMENT ON COLUMN yearly_fee_charge_marker.charge_year IS 'Calendar year for which the yearly fee was charged';
COMMENT ON COLUMN yearly_fee_charge_marker.charged_at IS 'Timestamp when the fee was successfully posted to finance module';

-- ============================================================================
-- BOOTSTRAP DATA NOTE
-- Bootstrap data (admin user and OAuth2 client) is managed by
-- BootstrapDataLoader component which reads credentials from environment variables.
-- This prevents credentials from being exposed in version control.
-- See: com.klabis.common.bootstrap.BootstrapDataLoader
-- ============================================================================
