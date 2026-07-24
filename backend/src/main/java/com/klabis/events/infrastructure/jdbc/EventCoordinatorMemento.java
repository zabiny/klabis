package com.klabis.events.infrastructure.jdbc;

import com.klabis.members.MemberId;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

/**
 * Memento for a single row in events.event_coordinators join table.
 * <p>
 * Spring Data JDBC maps this as a {@code Map<Integer, EventCoordinatorMemento>}
 * in {@link EventMemento}, using position as the map key column. This preserves
 * the insertion order of the {@link com.klabis.events.domain.Event#getCoordinators()}
 * LinkedHashSet across persistence boundaries.
 */
@Table(schema = "events", value = "event_coordinators")
class EventCoordinatorMemento {

    // event_id is managed by @MappedCollection idColumn in EventMemento

    @Column("member_id")
    private UUID memberId;

    // position is the Map key column — managed by @MappedCollection keyColumn

    protected EventCoordinatorMemento() {
    }

    static EventCoordinatorMemento of(MemberId memberId) {
        EventCoordinatorMemento m = new EventCoordinatorMemento();
        m.memberId = memberId.uuid();
        return m;
    }

    MemberId toMemberId() {
        return new MemberId(memberId);
    }
}
