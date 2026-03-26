-- Spring Modulith Infrastructure Tables
-- Created: 2026-02-09
-- Updated: 2026-03-26 — Modulith 2.0.0 schema (STATUS, COMPLETION_ATTEMPTS, LAST_RESUBMISSION_DATE)
--
-- These tables support Spring Modulith event-driven architecture
-- Used for event publication, outbox pattern, and async event handling
-- Not part of the core domain model

-- ============================================================================
-- EVENT_PUBLICATION TABLE
-- Spring Modulith Event Publication Outbox Table (schema v2 — Modulith 2.0+)
-- Stores domain events atomically with aggregate state changes
-- Events are persisted in the same transaction as the aggregate, then published asynchronously
-- Provides guaranteed at-least-once delivery with automatic retry
-- ============================================================================

CREATE TABLE event_publication
(
    -- Unique identifier for each event publication attempt
    id                     UUID          NOT NULL PRIMARY KEY,

    -- When the event was successfully processed (null if not yet completed)
    completion_date        TIMESTAMP WITH TIME ZONE,

    -- Fully qualified class name of the event type
    -- Example: com.klabis.members.domain.events.MemberCreatedEvent
    event_type             VARCHAR(512)  NOT NULL,

    -- Identifier of the event listener/method
    -- Example: com.klabis.members.registration.MemberCreatedEventHandler.onMemberCreated
    listener_id            VARCHAR(512)  NOT NULL,

    -- When the event was first persisted to the outbox
    publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,

    -- Serialized event object (JSON format)
    -- Contains all event data needed for processing
    -- Using VARCHAR(4000) which works for both H2 and PostgreSQL
    serialized_event       VARCHAR(4000) NOT NULL,

    -- Publication status (PUBLISHED, COMPLETED, FAILED)
    status                 VARCHAR(20),

    -- Number of delivery attempts made for this event
    completion_attempts    INT,

    -- Timestamp of the most recent resubmission attempt
    last_resubmission_date TIMESTAMP WITH TIME ZONE
);

-- Indexes for event_publication (Modulith 2.0 schema)
CREATE INDEX idx_event_publication_listener_serialized ON event_publication (listener_id, serialized_event);
CREATE INDEX idx_event_publication_completion_date ON event_publication (completion_date);
