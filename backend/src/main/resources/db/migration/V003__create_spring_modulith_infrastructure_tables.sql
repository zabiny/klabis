-- Spring Modulith Infrastructure Tables
-- Created: 2026-02-09
--
-- These tables support Spring Modulith event-driven architecture
-- Used for event publication, outbox pattern, and async event handling
-- Not part of the core domain model

-- ============================================================================
-- EVENT_PUBLICATION TABLE
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
    -- Using VARCHAR(4000) which works for both H2 and PostgreSQL
    -- PostgreSQL TEXT would be ideal but H2 interprets TEXT as VARCHAR(255)
    serialized_event VARCHAR(4000) NOT NULL,

    -- When the event was successfully processed (null if not yet completed)
    -- Allows tracking incomplete events for retry
    completion_date  TIMESTAMP
);

-- Indexes for event_publication
CREATE INDEX idx_event_publication_completion_date ON event_publication (completion_date);
CREATE INDEX idx_event_publication_publication_date ON event_publication (publication_date);

-- Comments for event_publication
COMMENT ON TABLE event_publication IS 'Spring Modulith event publication outbox - stores domain events for async processing';
COMMENT ON COLUMN event_publication.id IS 'Unique event publication identifier (UUID)';
COMMENT ON COLUMN event_publication.event_type IS 'Fully qualified class name of the event type (e.g., com.klabis.members.domain.events.MemberCreatedEvent)';
COMMENT ON COLUMN event_publication.listener_id IS 'Identifier of the event listener/method (e.g., com.klabis.members.registration.MemberCreatedEventHandler.onMemberCreated)';
COMMENT ON COLUMN event_publication.publication_date IS 'When the event was first persisted to the outbox';
COMMENT ON COLUMN event_publication.serialized_event IS 'Serialized event object in JSON format (VARCHAR(4000) for H2/PostgreSQL compatibility)';
COMMENT ON COLUMN event_publication.completion_date IS 'When the event was successfully processed (null if incomplete)';
