package com.klabis.events.infrastructure.restapi;

import com.klabis.events.DisciplineId;

import java.util.UUID;

interface DisciplineIdMapping {

    default DisciplineId toDisciplineId(UUID id) {
        return id == null ? null : new DisciplineId(id);
    }

    default UUID toUuid(DisciplineId id) {
        return id == null ? null : id.value();
    }
}
