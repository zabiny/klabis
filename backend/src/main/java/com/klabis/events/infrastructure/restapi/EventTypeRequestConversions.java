package com.klabis.events.infrastructure.restapi;

import com.klabis.events.DisciplineId;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Wire-to-domain (and back) conversions shared by the create/update event type mappers and the
 * DTO converter. The generated request/response records carry wire types only ({@code UUID}
 * rather than {@code DisciplineId}) — see the "DTOs carry wire types" rule in the klabis-api-spec
 * skill.
 */
final class EventTypeRequestConversions {

    private EventTypeRequestConversions() {}

    static Set<DisciplineId> toDisciplineIds(Collection<UUID> disciplineIds) {
        if (disciplineIds == null) {
            return null;
        }
        return disciplineIds.stream().map(DisciplineId::new).collect(Collectors.toSet());
    }

    static List<UUID> toDisciplineUuids(Set<DisciplineId> disciplineIds) {
        if (disciplineIds == null) {
            return List.of();
        }
        return disciplineIds.stream().map(DisciplineId::value).toList();
    }
}
