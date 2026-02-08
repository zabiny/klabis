-- Spring Modulith Event Publication Outbox Table
-- This table stores domain events atomically with aggregate state changes
-- Events are persisted in the same transaction as the aggregate, then published asynchronously
-- Provides guaranteed at-least-once delivery with automatic retry

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
    -- Using VARCHAR(4096) which works for both H2 and PostgreSQL
    -- PostgreSQL TEXT would be ideal but H2 interprets TEXT as VARCHAR(255)
    serialized_event VARCHAR(4096) NOT NULL,

    -- When the event was successfully processed (null if not yet completed)
    -- Allows tracking incomplete events for retry
    completion_date  TIMESTAMP
);

-- Index on completion_date for efficient cleanup queries
-- Spring Modulith periodically deletes completed events based on retention policy
CREATE INDEX idx_event_publication_completion_date ON event_publication (completion_date);

-- Index on publication_date for querying incomplete events older than threshold
-- Supports Spring Modulith's automatic republish of stale incomplete events
CREATE INDEX idx_event_publication_publication_date ON event_publication (publication_date);
