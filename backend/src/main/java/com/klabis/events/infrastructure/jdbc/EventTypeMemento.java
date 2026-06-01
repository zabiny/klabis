package com.klabis.events.infrastructure.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.events.EventTypeId;
import com.klabis.events.domain.EventType;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Table("event_types")
class EventTypeMemento implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("name")
    private String name;

    @Column("color")
    private String color;

    @Column("sort_order")
    private int sortOrder;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @LastModifiedDate
    @Column("modified_at")
    private Instant lastModifiedAt;

    @LastModifiedBy
    @Column("modified_by")
    private String lastModifiedBy;

    @Version
    @Column("version")
    private Long version;

    @MappedCollection(idColumn = "event_type_id")
    private Set<OrisDisciplineMemento> orisDisciplines = new HashSet<>();

    @Transient
    private boolean isNew = true;

    protected EventTypeMemento() {
    }

    static EventTypeMemento from(EventType eventType) {
        EventTypeMemento memento = new EventTypeMemento();
        memento.id = eventType.getId().value();
        memento.name = eventType.getName();
        memento.color = eventType.getColor().orElse(null);
        memento.sortOrder = eventType.getSortOrder();
        memento.orisDisciplines = eventType.getOrisDisciplineIds().stream()
                .map(OrisDisciplineMemento::of)
                .collect(Collectors.toCollection(HashSet::new));

        memento.createdAt = eventType.getCreatedAt();
        memento.createdBy = eventType.getCreatedBy();
        memento.lastModifiedAt = eventType.getLastModifiedAt();
        memento.lastModifiedBy = eventType.getLastModifiedBy();

        memento.isNew = (eventType.getAuditMetadata() == null);
        return memento;
    }

    EventType toEventType() {
        Set<Integer> disciplineIds = this.orisDisciplines.stream()
                .map(OrisDisciplineMemento::getDisciplineId)
                .collect(Collectors.toSet());
        return EventType.reconstruct(
                new EventTypeId(this.id),
                this.name,
                this.color,
                this.sortOrder,
                new AuditMetadata(this.createdAt, this.createdBy, this.lastModifiedAt, this.lastModifiedBy, this.version),
                disciplineIds
        );
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
