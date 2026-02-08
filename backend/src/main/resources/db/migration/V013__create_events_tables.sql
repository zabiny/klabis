-- Create events module tables
-- Stores event information and member registrations for events

-- Events table
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

-- Event registrations table
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

-- Indexes for events table
CREATE INDEX idx_events_status ON events (status);
CREATE INDEX idx_events_event_date ON events (event_date);
CREATE INDEX idx_events_organizer ON events (organizer);

-- Indexes for event_registrations table
CREATE INDEX idx_event_registrations_event_id ON event_registrations (event_id);

-- Comments for events table
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

-- Comments for event_registrations table
COMMENT ON TABLE event_registrations IS 'Stores member registrations for events';
COMMENT ON COLUMN event_registrations.id IS 'Unique registration identifier (UUID)';
COMMENT ON COLUMN event_registrations.event_id IS 'Reference to the event';
COMMENT ON COLUMN event_registrations.member_id IS 'Reference to the registered member';
COMMENT ON COLUMN event_registrations.si_card_number IS 'SI (SportIdent) card number used for the event';
COMMENT ON COLUMN event_registrations.registered_at IS 'Timestamp when member registered for the event';
