package com.klabis.events.infrastructure.jdbc;

import com.klabis.events.DisciplineId;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table(schema = "events", value = "event_type_disciplines")
class EventTypeDisciplineMemento {

    // event_type_id is managed by @MappedCollection in EventTypeMemento — Spring Data JDBC sets it automatically

    @Column("discipline_id")
    private UUID disciplineId;

    protected EventTypeDisciplineMemento() {
    }

    static EventTypeDisciplineMemento of(DisciplineId disciplineId) {
        EventTypeDisciplineMemento m = new EventTypeDisciplineMemento();
        m.disciplineId = disciplineId.value();
        return m;
    }

    DisciplineId getDisciplineId() {
        return new DisciplineId(disciplineId);
    }
}
