package com.klabis.events.infrastructure.orissync;

import com.dpolach.api.orisclient.dto.lov.DisciplineListEntry;
import com.klabis.events.domain.Discipline;

/**
 * Maps both sides of a {@code Discipline} synchronisation into the canonical
 * {@link DisciplineProjection} shape (design.md D3): the local
 * {@link Discipline} aggregate, and ORIS's {@link DisciplineListEntry} (the only
 * shape {@code OrisApiClient.listDisciplines()} offers — ORIS has no
 * get-one-discipline endpoint, so {@link DisciplineSyncAdapter#readExternal} finds
 * the matching entry out of the full list).
 * <p>
 * {@code DisciplineListEntry.name()} carries ORIS's short technical designation
 * (e.g. {@code "OB"}) and {@code descriptionCZ()} the human-readable Czech name
 * (e.g. {@code "Orientační běh"}) — the same "prefer descriptionCZ, fall back to
 * name" rule already established for {@code LevelListEntry} in
 * {@code OrisRankingOptionsAdapter}, applied here to fill {@code name} when ORIS
 * sends no description for an entry.
 */
final class DisciplineProjectionMapper {

    private DisciplineProjectionMapper() {
    }

    static DisciplineProjection fromDiscipline(Discipline discipline) {
        return new DisciplineProjection(discipline.getCode(), discipline.getName());
    }

    static DisciplineProjection fromOrisEntry(DisciplineListEntry entry) {
        String name = (entry.descriptionCZ() != null && !entry.descriptionCZ().isBlank())
                ? entry.descriptionCZ()
                : entry.name();
        return new DisciplineProjection(entry.name(), name);
    }
}
