package com.klabis.usergroups.infrastructure.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.domain.FreeGroup;
import com.klabis.usergroups.domain.GroupMembership;
import com.klabis.usergroups.domain.UserGroup;
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

@Table("user_groups")
class UserGroupMemento implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("type")
    private String type;

    @Column("name")
    private String name;

    @MappedCollection(idColumn = "user_group_id")
    private Set<UserGroupOwnerMemento> owners = new HashSet<>();

    @MappedCollection(idColumn = "user_group_id")
    private Set<UserGroupMemberMemento> members = new HashSet<>();

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

    protected UserGroupMemento() {
    }

    static UserGroupMemento from(UserGroup group) {
        UserGroupMemento memento = new UserGroupMemento();
        memento.id = group.getId().uuid();
        memento.name = group.getName();
        memento.type = discriminatorFor(group);

        memento.owners = group.getOwners().stream()
                .map(memberId -> new UserGroupOwnerMemento(memberId.uuid()))
                .collect(Collectors.toSet());

        memento.members = group.getMembers().stream()
                .map(m -> new UserGroupMemberMemento(m.memberId().uuid(), m.joinedAt()))
                .collect(Collectors.toSet());

        if (group.getAuditMetadata() != null) {
            memento.createdAt = group.getCreatedAt();
            memento.createdBy = group.getCreatedBy();
            memento.lastModifiedAt = group.getLastModifiedAt();
            memento.lastModifiedBy = group.getLastModifiedBy();
            memento.version = group.getVersion();
        }

        memento.isNew = (group.getAuditMetadata() == null);
        return memento;
    }

    UserGroup toUserGroup() {
        UserGroupId groupId = new UserGroupId(this.id);

        Set<MemberId> ownerIds = owners.stream()
                .map(o -> new MemberId(o.getMemberId()))
                .collect(Collectors.toSet());

        Set<GroupMembership> memberships = members.stream()
                .map(m -> new GroupMembership(new MemberId(m.getMemberId()), m.getJoinedAt()))
                .collect(Collectors.toSet());

        AuditMetadata auditMetadata = this.createdAt != null
                ? new AuditMetadata(this.createdAt, this.createdBy, this.lastModifiedAt, this.lastModifiedBy, this.version)
                : null;

        return switch (this.type) {
            case FreeGroup.TYPE_DISCRIMINATOR -> FreeGroup.reconstruct(groupId, this.name, ownerIds, memberships, auditMetadata);
            default -> throw new IllegalStateException("Unknown user group type: " + this.type);
        };
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    private static String discriminatorFor(UserGroup group) {
        if (group instanceof FreeGroup) {
            return FreeGroup.TYPE_DISCRIMINATOR;
        }
        throw new IllegalArgumentException("Unknown UserGroup subtype: " + group.getClass().getSimpleName());
    }
}
