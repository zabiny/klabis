package com.klabis.events.infrastructure.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.events.DisciplineId;
import com.klabis.events.domain.Discipline;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
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

    protected DisciplineMemento() {
    }

    static DisciplineMemento from(Discipline discipline) {
        DisciplineMemento memento = new DisciplineMemento();
        memento.id = discipline.getId().value();
        memento.code = discipline.getCode();
        memento.name = discipline.getName();
        memento.archived = false;

        memento.createdAt = discipline.getCreatedAt();
        memento.createdBy = discipline.getCreatedBy();
        memento.lastModifiedAt = discipline.getLastModifiedAt();
        memento.lastModifiedBy = discipline.getLastModifiedBy();

        memento.isNew = (discipline.getAuditMetadata() == null);
        return memento;
    }

    Discipline toDiscipline() {
        return Discipline.reconstruct(
                new DisciplineId(this.id),
                this.code,
                this.name,
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
}
