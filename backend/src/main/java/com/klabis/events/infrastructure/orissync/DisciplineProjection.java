package com.klabis.events.infrastructure.orissync;

import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncProjection;

/**
 * The ORIS-owned fields of a {@code Discipline} (design.md D3): {@code code} and
 * {@code name}, the only fields a {@code Discipline} has. Unlike
 * {@link OrisEventProjection}, there is no Klabis-owned field to omit and no
 * write-time-only value to smuggle through a {@code @JsonIgnore} component — a
 * discipline carries no local identity of its own beyond its generated
 * {@code DisciplineId}, and the ORIS pairing lives entirely in the {@code sync}
 * module's {@code SyncRecord} (design.md D1).
 */
public record DisciplineProjection(
        String code,
        String name
) implements SyncProjection {

    @Override
    public SyncEntityType entityType() {
        return SyncEntityType.DISCIPLINE;
    }
}
