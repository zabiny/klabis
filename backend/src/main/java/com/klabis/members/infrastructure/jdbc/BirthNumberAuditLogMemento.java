package com.klabis.members.infrastructure.jdbc;

import com.klabis.members.BirthNumberAccessedEvent.BirthNumberAction;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("birth_number_audit_log")
class BirthNumberAuditLogMemento implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("user_id")
    private UUID userId;

    @Column("member_id")
    private UUID memberId;

    @Column("action")
    private BirthNumberAction action;

    @Column("occurred_at")
    private Instant occurredAt;

    protected BirthNumberAuditLogMemento() {
    }

    static BirthNumberAuditLogMemento of(UUID userId, UUID memberId, BirthNumberAction action, Instant occurredAt) {
        BirthNumberAuditLogMemento memento = new BirthNumberAuditLogMemento();
        memento.id = UUID.randomUUID();
        memento.userId = userId;
        memento.memberId = memberId;
        memento.action = action;
        memento.occurredAt = occurredAt;
        return memento;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return true;
    }
}
