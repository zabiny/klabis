package com.klabis.members.legalguardian.infrastructure.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.users.UserId;
import com.klabis.members.domain.EmailAddress;
import com.klabis.members.domain.PersonName;
import com.klabis.members.domain.PhoneNumber;
import com.klabis.members.legalguardian.domain.LegalGuardian;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Table(schema = "members", value = "legal_guardians")
class LegalGuardianMemento implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    @Column("email")
    private String email;

    @Column("phone")
    private String phone;

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
    private LegalGuardian legalGuardian;

    @Transient
    private boolean isNew = true;

    protected LegalGuardianMemento() {
    }

    static LegalGuardianMemento from(LegalGuardian legalGuardian) {
        LegalGuardianMemento memento = new LegalGuardianMemento();
        memento.id = legalGuardian.getId().uuid();
        memento.firstName = legalGuardian.getFirstName();
        memento.lastName = legalGuardian.getLastName();
        memento.email = legalGuardian.getEmail().value();
        memento.phone = legalGuardian.getPhone().value();

        if (legalGuardian.getAuditMetadata() != null) {
            memento.createdAt = legalGuardian.getCreatedAt();
            memento.createdBy = legalGuardian.getCreatedBy();
            memento.lastModifiedAt = legalGuardian.getLastModifiedAt();
            memento.lastModifiedBy = legalGuardian.getLastModifiedBy();
            memento.version = legalGuardian.getVersion();
        }

        memento.legalGuardian = legalGuardian;
        memento.isNew = legalGuardian.getAuditMetadata() == null;
        return memento;
    }

    LegalGuardian toLegalGuardian() {
        LegalGuardian guardian = LegalGuardian.reconstruct(new UserId(id),
                PersonName.of(firstName, lastName),
                EmailAddress.of(email),
                PhoneNumber.of(phone),
                auditMetadata());
        this.legalGuardian = guardian;
        return guardian;
    }

    @DomainEvents
    List<Object> domainEvents() {
        return legalGuardian != null ? legalGuardian.getDomainEvents() : List.of();
    }

    @AfterDomainEventPublication
    void clearDomainEvents() {
        if (legalGuardian != null) {
            legalGuardian.clearDomainEvents();
        }
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public UUID getId() {
        return id;
    }

    private AuditMetadata auditMetadata() {
        if (createdAt == null) {
            return null;
        }
        return new AuditMetadata(createdAt, createdBy, lastModifiedAt, lastModifiedBy, version);
    }
}
