package com.klabis.events.infrastructure.jdbc;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(schema = "events", value = "event_type_oris_disciplines")
class OrisDisciplineMemento {

    // event_type_id is managed by @MappedCollection in EventTypeMemento — Spring Data JDBC sets it automatically

    @Column("discipline_id")
    private int disciplineId;

    protected OrisDisciplineMemento() {
    }

    static OrisDisciplineMemento of(int disciplineId) {
        OrisDisciplineMemento m = new OrisDisciplineMemento();
        m.disciplineId = disciplineId;
        return m;
    }

    int getDisciplineId() {
        return disciplineId;
    }
}
