package com.klabis.members.membersgroup.infrastructure.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.usergroup.GroupMembership;
import com.klabis.common.usergroup.Invitation;
import com.klabis.common.usergroup.InvitationId;
import com.klabis.common.usergroup.InvitationStatus;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.membersgroup.domain.MembersGroup;
import com.klabis.members.membersgroup.domain.MembersGroupId;
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

@Table("members_groups")
class MembersGroupMemento implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("name")
    private String name;

    @MappedCollection(idColumn = "members_group_id")
    private Set<MembersGroupOwnerMemento> owners = new HashSet<>();

    @MappedCollection(idColumn = "members_group_id")
    private Set<MembersGroupMemberMemento> members = new HashSet<>();

    @MappedCollection(idColumn = "members_group_id")
    private Set<MembersGroupInvitationMemento> invitations = new HashSet<>();

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
    private MembersGroup membersGroup;

    @Transient
    private boolean isNew = true;

    protected MembersGroupMemento() {
    }

    static MembersGroupMemento from(MembersGroup group) {
        MembersGroupMemento memento = new MembersGroupMemento();
        memento.id = group.getId().value();
        memento.name = group.getName();

        memento.owners = group.getOwners().stream()
                .map(memberId -> new MembersGroupOwnerMemento(memberId.value()))
                .collect(Collectors.toSet());

        memento.members = group.getMembers().stream()
                .map(m -> new MembersGroupMemberMemento(m.userId().uuid(), m.joinedAt()))
                .collect(Collectors.toSet());

        memento.invitations = group.getInvitations().stream()
                .map(inv -> new MembersGroupInvitationMemento(
                        inv.getId().value(),
                        inv.getInvitedUser().uuid(),
                        inv.getInvitedBy().uuid(),
                        inv.getStatus().name(),
                        inv.getCreatedAt()))
                .collect(Collectors.toSet());

        if (group.getAuditMetadata() != null) {
            memento.createdAt = group.getCreatedAt();
            memento.createdBy = group.getCreatedBy();
            memento.lastModifiedAt = group.getLastModifiedAt();
            memento.lastModifiedBy = group.getLastModifiedBy();
            memento.version = group.getVersion();
        }

        memento.membersGroup = group;
        memento.isNew = (group.getAuditMetadata() == null);
        return memento;
    }

    MembersGroup toMembersGroup() {
        MembersGroupId groupId = new MembersGroupId(this.id);

        Set<MemberId> ownerIds = owners.stream()
                .map(o -> new MemberId(o.getMemberId()))
                .collect(Collectors.toSet());

        Set<GroupMembership> memberships = members.stream()
                .map(m -> new GroupMembership(new UserId(m.getMemberId()), m.getJoinedAt()))
                .collect(Collectors.toSet());

        Set<Invitation> invitationSet = invitations.stream()
                .map(inv -> Invitation.reconstruct(
                        new InvitationId(inv.getId()),
                        new UserId(inv.getInvitedMemberId()),
                        new UserId(inv.getInvitedByMemberId()),
                        InvitationStatus.valueOf(inv.getStatus()),
                        inv.getCreatedAt()))
                .collect(Collectors.toSet());

        AuditMetadata auditMetadata = this.createdAt != null
                ? new AuditMetadata(this.createdAt, this.createdBy, this.lastModifiedAt, this.lastModifiedBy, this.version)
                : null;

        return MembersGroup.reconstruct(groupId, this.name, ownerIds, memberships, invitationSet, auditMetadata);
    }

    @DomainEvents
    public List<Object> getDomainEvents() {
        return this.membersGroup != null ? this.membersGroup.getDomainEvents() : List.of();
    }

    @AfterDomainEventPublication
    public void clearDomainEvents() {
        if (this.membersGroup != null) {
            this.membersGroup.clearDomainEvents();
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
