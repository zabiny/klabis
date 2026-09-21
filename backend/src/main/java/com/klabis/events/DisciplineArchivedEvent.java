package com.klabis.events;

import com.klabis.events.domain.Discipline;
import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when a Discipline is archived.
 *
 * <p>Consumed by {@code DisciplineSyncListener} to retire the discipline's sync
 * pairing with ORIS (design.md D8 of {@code sync-oris-disciplines}) — a no-op when
 * the discipline was never paired (manually created).
 *
 * <p>Domain events are immutable and represent facts that have already occurred.
 */
@DomainEvent
public record DisciplineArchivedEvent(
        UUID occurrenceId,
        DisciplineId disciplineId,
        Instant occurredAt
) {

    public DisciplineArchivedEvent {
        Objects.requireNonNull(occurrenceId, "Occurrence ID is required");
        Objects.requireNonNull(disciplineId, "Discipline ID is required");
        Objects.requireNonNull(occurredAt, "Occurred at timestamp is required");
    }

    public static DisciplineArchivedEvent fromAggregate(Discipline discipline) {
        return new DisciplineArchivedEvent(UUID.randomUUID(), discipline.getId(), Instant.now());
    }
}
