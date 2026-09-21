package com.klabis.events.infrastructure.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.events.DisciplineId;
import com.klabis.events.domain.Discipline;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Table(schema = "events", value = "disciplines")
class DisciplineMemento implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("code")
    private String code;

    @Column("name")
    private String name;

    @Column("archived")
    private boolean archived;

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

    @Transient
    private boolean isNew = true;

    // Transient reference to Discipline for domain event delegation (archive()
    // registers DisciplineArchivedEvent — without this, Spring Data JDBC never
    // publishes it, and DisciplineSyncListener never fires).
    @Transient
    private Discipline discipline;

    protected DisciplineMemento() {
    }

    static DisciplineMemento from(Discipline discipline) {
        DisciplineMemento memento = new DisciplineMemento();
        memento.id = discipline.getId().value();
        memento.code = discipline.getCode();
        memento.name = discipline.getName();
        memento.archived = discipline.isArchived();

        memento.createdAt = discipline.getCreatedAt();
        memento.createdBy = discipline.getCreatedBy();
        memento.lastModifiedAt = discipline.getLastModifiedAt();
        memento.lastModifiedBy = discipline.getLastModifiedBy();

        memento.isNew = (discipline.getAuditMetadata() == null);
        memento.discipline = discipline;
        return memento;
    }

    Discipline toDiscipline() {
        return Discipline.reconstruct(
                new DisciplineId(this.id),
                this.code,
                this.name,
                this.archived,
                new AuditMetadata(this.createdAt, this.createdBy, this.lastModifiedAt, this.lastModifiedBy, this.version)
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

    @DomainEvents
    List<Object> domainEvents() {
        return discipline != null ? discipline.getDomainEvents() : List.of();
    }

    @AfterDomainEventPublication
    void clearDomainEvents() {
        if (discipline != null) {
            discipline.clearDomainEvents();
        }
    }
}
