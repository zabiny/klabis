package com.klabis.events.infrastructure.jdbc;

import com.klabis.events.DisciplineId;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table(schema = "events", value = "event_type_oris_disciplines")
class OrisDisciplineMemento {

    // event_type_id is managed by @MappedCollection in EventTypeMemento — Spring Data JDBC sets it automatically

    @Column("discipline_id")
    private UUID disciplineId;

    protected OrisDisciplineMemento() {
    }

    static OrisDisciplineMemento of(DisciplineId disciplineId) {
        OrisDisciplineMemento m = new OrisDisciplineMemento();
        m.disciplineId = disciplineId.value();
        return m;
    }

    DisciplineId getDisciplineId() {
        return new DisciplineId(disciplineId);
    }
}
