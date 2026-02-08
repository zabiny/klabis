-- Increase serialized_event column size to handle larger event payloads
-- Domain events with complex aggregates can exceed 255 characters
-- This migration works for both H2 and PostgreSQL

ALTER TABLE event_publication
    ALTER COLUMN serialized_event TYPE VARCHAR(4000);
