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
-- 5. events (FK → members)
-- 6. event_registrations (FK → events, members)
-- 7. calendar_items (FK → events)
-- 8. birth_number_audit_log (no FK)
-- 9. training_groups + training_group_trainers + training_group_members
-- 10. family_groups + family_group_parents + family_group_children
-- 11. members_groups + members_group_owners + members_group_members + members_group_invitations
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
-- 5. EVENTS TABLE
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

    -- Registration deadline: last date when members can register/unregister (null means no deadline)
    registration_deadline DATE         NULL,

    -- ORIS integration: source identifier for imported events (null for manually created events)
    oris_id              INTEGER      NULL UNIQUE,

    -- Race categories available at this event (comma-separated, e.g. "M21,W35,D10"; null means no categories defined)
    categories           VARCHAR(2000) NULL,

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
COMMENT ON COLUMN events.registration_deadline IS 'Optional last date when members can register or unregister; null means no deadline';
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
    id               UUID PRIMARY KEY,
    name             VARCHAR(200) NOT NULL,
    description      TEXT         NOT NULL,
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
-- 14. TRAINING_GROUPS TABLE
-- Stores training groups as an independent aggregate root in the members module
-- ============================================================================

CREATE TABLE training_groups
(
    id            UUID         PRIMARY KEY,
    name          VARCHAR(200) NOT NULL,
    age_range_min INT          NOT NULL,
    age_range_max INT          NOT NULL,

    -- Audit fields
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(100) NOT NULL,
    modified_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by   VARCHAR(100) NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0
);

-- Indexes for training_groups
CREATE INDEX idx_training_groups_name ON training_groups (name);

-- Comments for training_groups
COMMENT ON TABLE training_groups IS 'Training groups as independent aggregate root in the members module';
COMMENT ON COLUMN training_groups.age_range_min IS 'Minimum age for this training group (inclusive)';
COMMENT ON COLUMN training_groups.age_range_max IS 'Maximum age for this training group (inclusive)';

-- ============================================================================
-- 15. TRAINING_GROUP_TRAINERS TABLE
-- Maps trainers (members acting as owners) to training groups
-- ============================================================================

CREATE TABLE training_group_trainers
(
    training_group_id UUID NOT NULL REFERENCES training_groups (id) ON DELETE CASCADE,
    member_id         UUID NOT NULL,
    PRIMARY KEY (training_group_id, member_id)
);

-- Indexes for training_group_trainers
CREATE INDEX idx_training_group_trainers_group_id ON training_group_trainers (training_group_id);
CREATE INDEX idx_training_group_trainers_member_id ON training_group_trainers (member_id);

-- Comments for training_group_trainers
COMMENT ON TABLE training_group_trainers IS 'Maps trainers (members acting as owners) to training groups';

-- ============================================================================
-- 16. TRAINING_GROUP_MEMBERS TABLE
-- Maps members to training groups with join timestamp
-- ============================================================================

CREATE TABLE training_group_members
(
    training_group_id UUID      NOT NULL REFERENCES training_groups (id) ON DELETE CASCADE,
    member_id         UUID      NOT NULL,
    joined_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (training_group_id, member_id)
);

-- Indexes for training_group_members
CREATE INDEX idx_training_group_members_group_id ON training_group_members (training_group_id);
CREATE INDEX idx_training_group_members_member_id ON training_group_members (member_id);

-- Comments for training_group_members
COMMENT ON TABLE training_group_members IS 'Maps members to training groups with join timestamp';
COMMENT ON COLUMN training_group_members.joined_at IS 'Timestamp when member was assigned to the training group';

-- ============================================================================
-- 17. FAMILY_GROUPS TABLE
-- Family groups as independent aggregate root in the members module
-- ============================================================================

CREATE TABLE family_groups
(
    id          UUID         NOT NULL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(100) NOT NULL,
    modified_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR(100) NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0
);

-- Indexes for family_groups
CREATE INDEX idx_family_groups_name ON family_groups (name);

-- Comments for family_groups
COMMENT ON TABLE family_groups IS 'Family groups as independent aggregate root in the members module';

-- ============================================================================
-- 18. FAMILY_GROUP_PARENTS TABLE
-- Maps parents (members acting as owners) to family groups
-- ============================================================================

CREATE TABLE family_group_parents
(
    family_group_id UUID NOT NULL REFERENCES family_groups (id) ON DELETE CASCADE,
    member_id       UUID NOT NULL,
    PRIMARY KEY (family_group_id, member_id)
);

-- Indexes for family_group_parents
CREATE INDEX idx_family_group_parents_group_id ON family_group_parents (family_group_id);
CREATE INDEX idx_family_group_parents_member_id ON family_group_parents (member_id);

-- Comments for family_group_parents
COMMENT ON TABLE family_group_parents IS 'Maps parents (members acting as owners) to family groups';

-- ============================================================================
-- 19. FAMILY_GROUP_CHILDREN TABLE
-- Maps all members (parents and children) to family groups with join timestamp
-- ============================================================================

CREATE TABLE family_group_children
(
    family_group_id UUID      NOT NULL REFERENCES family_groups (id) ON DELETE CASCADE,
    member_id       UUID      NOT NULL,
    joined_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (family_group_id, member_id)
);

-- Indexes for family_group_children
CREATE INDEX idx_family_group_children_group_id ON family_group_children (family_group_id);
CREATE INDEX idx_family_group_children_member_id ON family_group_children (member_id);

-- Comments for family_group_children
COMMENT ON TABLE family_group_children IS 'Maps all members (parents included) to family groups with join timestamp';
COMMENT ON COLUMN family_group_children.joined_at IS 'Timestamp when member was added to the family group';

-- ============================================================================
-- 20. MEMBERS_GROUPS TABLE
-- Members groups (invitation-based free groups) as independent aggregate root in the members module
-- ============================================================================

CREATE TABLE members_groups
(
    id          UUID         NOT NULL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(100) NOT NULL,
    modified_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR(100) NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0
);

-- Indexes for members_groups
CREATE INDEX idx_members_groups_name ON members_groups (name);

-- Comments for members_groups
COMMENT ON TABLE members_groups IS 'Invitation-based members groups as independent aggregate root in the members module';

-- ============================================================================
-- 21. MEMBERS_GROUP_OWNERS TABLE
-- Maps owners (group admins) to members groups
-- ============================================================================

CREATE TABLE members_group_owners
(
    members_group_id UUID NOT NULL REFERENCES members_groups (id) ON DELETE CASCADE,
    member_id        UUID NOT NULL,
    PRIMARY KEY (members_group_id, member_id)
);

-- Indexes for members_group_owners
CREATE INDEX idx_members_group_owners_group_id ON members_group_owners (members_group_id);
CREATE INDEX idx_members_group_owners_member_id ON members_group_owners (member_id);

-- Comments for members_group_owners
COMMENT ON TABLE members_group_owners IS 'Maps owners (group admins) to members groups';

-- ============================================================================
-- 22. MEMBERS_GROUP_MEMBERS TABLE
-- Maps members to members groups with join timestamp
-- ============================================================================

CREATE TABLE members_group_members
(
    members_group_id UUID      NOT NULL REFERENCES members_groups (id) ON DELETE CASCADE,
    member_id        UUID      NOT NULL,
    joined_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (members_group_id, member_id)
);

-- Indexes for members_group_members
CREATE INDEX idx_members_group_members_group_id ON members_group_members (members_group_id);
CREATE INDEX idx_members_group_members_member_id ON members_group_members (member_id);

-- Comments for members_group_members
COMMENT ON TABLE members_group_members IS 'Maps members to members groups with join timestamp';
COMMENT ON COLUMN members_group_members.joined_at IS 'Timestamp when member was added to the group (via accepted invitation)';

-- ============================================================================
-- 23. MEMBERS_GROUP_INVITATIONS TABLE
-- Stores membership invitations for members groups
-- ============================================================================

CREATE TABLE members_group_invitations
(
    id                   UUID        PRIMARY KEY,
    members_group_id     UUID        NOT NULL REFERENCES members_groups (id) ON DELETE CASCADE,
    invited_member_id    UUID        NOT NULL,
    invited_by_member_id UUID        NOT NULL,
    status               VARCHAR(20) NOT NULL,
    created_at           TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for members_group_invitations
CREATE INDEX idx_members_group_invitations_group_id ON members_group_invitations (members_group_id);
CREATE INDEX idx_members_group_invitations_invited_member_id ON members_group_invitations (invited_member_id);
CREATE INDEX idx_members_group_invitations_status ON members_group_invitations (status);

-- Comments for members_group_invitations
COMMENT ON TABLE members_group_invitations IS 'Membership invitations for members groups';
COMMENT ON COLUMN members_group_invitations.status IS 'Invitation status: PENDING, ACCEPTED, REJECTED';

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
-- BOOTSTRAP DATA NOTE
-- Bootstrap data (admin user and OAuth2 client) is managed by
-- BootstrapDataLoader component which reads credentials from environment variables.
-- This prevents credentials from being exposed in version control.
-- See: com.klabis.common.bootstrap.BootstrapDataLoader
-- ============================================================================
