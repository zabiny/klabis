package com.klabis.members.familygroup.infrastructure.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.usergroup.GroupMembership;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.familygroup.domain.FamilyGroup;
import com.klabis.members.familygroup.domain.FamilyGroupId;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Table("family_groups")
class FamilyGroupMemento implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("name")
    private String name;

    @MappedCollection(idColumn = "family_group_id")
    private Set<FamilyGroupParentMemento> parents = new HashSet<>();

    // Stores all members (parents included, as parents are owners + members in this model)
    @MappedCollection(idColumn = "family_group_id")
    private Set<FamilyGroupMemberMemento> members = new HashSet<>();

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
    private FamilyGroup familyGroup;

    @Transient
    private boolean isNew = true;

    protected FamilyGroupMemento() {
    }

    static FamilyGroupMemento from(FamilyGroup group) {
        FamilyGroupMemento memento = new FamilyGroupMemento();
        memento.id = group.getId().value();
        memento.name = group.getName();

        memento.parents = group.getParents().stream()
                .map(memberId -> new FamilyGroupParentMemento(memberId.value()))
                .collect(Collectors.toSet());

        memento.members = group.getMembers().stream()
                .map(m -> new FamilyGroupMemberMemento(m.userId().uuid(), m.joinedAt()))
                .collect(Collectors.toSet());

        if (group.getAuditMetadata() != null) {
            memento.createdAt = group.getCreatedAt();
            memento.createdBy = group.getCreatedBy();
            memento.lastModifiedAt = group.getLastModifiedAt();
            memento.lastModifiedBy = group.getLastModifiedBy();
            memento.version = group.getVersion();
        }

        memento.familyGroup = group;
        memento.isNew = (group.getAuditMetadata() == null);
        return memento;
    }

    FamilyGroup toFamilyGroup() {
        FamilyGroupId groupId = new FamilyGroupId(this.id);

        Set<MemberId> parentIds = parents.stream()
                .map(p -> new MemberId(p.getMemberId()))
                .collect(Collectors.toSet());

        Set<GroupMembership> memberships = members.stream()
                .map(c -> new GroupMembership(new UserId(c.getMemberId()), c.getJoinedAt()))
                .collect(Collectors.toSet());

        AuditMetadata auditMetadata = this.createdAt != null
                ? new AuditMetadata(this.createdAt, this.createdBy, this.lastModifiedAt, this.lastModifiedBy, this.version)
                : null;

        return FamilyGroup.reconstruct(groupId, this.name, parentIds, memberships, auditMetadata);
    }

    @DomainEvents
    public List<Object> getDomainEvents() {
        return this.familyGroup != null ? this.familyGroup.getDomainEvents() : List.of();
    }

    @AfterDomainEventPublication
    public void clearDomainEvents() {
        if (this.familyGroup != null) {
            this.familyGroup.clearDomainEvents();
        }
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
