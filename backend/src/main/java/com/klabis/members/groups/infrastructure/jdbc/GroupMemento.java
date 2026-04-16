package com.klabis.members.groups.infrastructure.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.usergroup.GroupMembership;
import com.klabis.common.usergroup.Invitation;
import com.klabis.common.usergroup.InvitationId;
import com.klabis.common.usergroup.InvitationStatus;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.familygroup.domain.FamilyGroup;
import com.klabis.members.familygroup.domain.FamilyGroupId;
import com.klabis.members.membersgroup.domain.MembersGroup;
import com.klabis.members.membersgroup.domain.MembersGroupId;
import com.klabis.members.traininggroup.domain.AgeRange;
import com.klabis.members.traininggroup.domain.TrainingGroup;
import com.klabis.members.traininggroup.domain.TrainingGroupId;
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

@Table("user_groups")
public class GroupMemento implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("type")
    private String type;

    @Column("name")
    private String name;

    @Column("age_range_min")
    private Integer ageRangeMin;

    @Column("age_range_max")
    private Integer ageRangeMax;

    @MappedCollection(idColumn = "user_group_id")
    private Set<GroupOwnerMemento> owners = new HashSet<>();

    @MappedCollection(idColumn = "user_group_id")
    private Set<GroupMemberMemento> members = new HashSet<>();

    @MappedCollection(idColumn = "user_group_id")
    private Set<GroupInvitationMemento> invitations = new HashSet<>();

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
    private Object aggregate;

    @Transient
    private boolean isNew = true;

    protected GroupMemento() {
    }

    public static GroupMemento fromMembersGroup(MembersGroup group) {
        GroupMemento memento = new GroupMemento();
        memento.id = group.getId().value();
        memento.type = MembersGroup.TYPE_DISCRIMINATOR;
        memento.name = group.getName();

        memento.owners = group.getOwners().stream()
                .map(memberId -> new GroupOwnerMemento(memberId.value()))
                .collect(Collectors.toSet());

        memento.members = group.getMembers().stream()
                .map(m -> new GroupMemberMemento(m.userId().uuid(), m.joinedAt()))
                .collect(Collectors.toSet());

        memento.invitations = group.getInvitations().stream()
                .map(inv -> new GroupInvitationMemento(
                        inv.getId().value(),
                        inv.getInvitedUser().uuid(),
                        inv.getInvitedBy().uuid(),
                        inv.getStatus().name(),
                        inv.getCreatedAt()))
                .collect(Collectors.toSet());

        memento.applyAudit(group.getAuditMetadata());
        memento.aggregate = group;
        memento.isNew = (group.getAuditMetadata() == null);
        return memento;
    }

    public static GroupMemento fromTrainingGroup(TrainingGroup group) {
        GroupMemento memento = new GroupMemento();
        memento.id = group.getId().value();
        memento.type = TrainingGroup.TYPE_DISCRIMINATOR;
        memento.name = group.getName();
        memento.ageRangeMin = group.getAgeRange().minAge();
        memento.ageRangeMax = group.getAgeRange().maxAge();

        memento.owners = group.getTrainers().stream()
                .map(memberId -> new GroupOwnerMemento(memberId.uuid()))
                .collect(Collectors.toSet());

        memento.members = group.getMembers().stream()
                .map(m -> new GroupMemberMemento(m.userId().uuid(), m.joinedAt()))
                .collect(Collectors.toSet());

        memento.applyAudit(group.getAuditMetadata());
        memento.aggregate = group;
        memento.isNew = (group.getAuditMetadata() == null);
        return memento;
    }

    public static GroupMemento fromFamilyGroup(FamilyGroup group) {
        GroupMemento memento = new GroupMemento();
        memento.id = group.getId().value();
        memento.type = FamilyGroup.TYPE_DISCRIMINATOR;
        memento.name = group.getName();

        memento.owners = group.getParents().stream()
                .map(memberId -> new GroupOwnerMemento(memberId.value()))
                .collect(Collectors.toSet());

        memento.members = group.getMembers().stream()
                .map(m -> new GroupMemberMemento(m.userId().uuid(), m.joinedAt()))
                .collect(Collectors.toSet());

        memento.applyAudit(group.getAuditMetadata());
        memento.aggregate = group;
        memento.isNew = (group.getAuditMetadata() == null);
        return memento;
    }

    public MembersGroup toMembersGroup() {
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

        AuditMetadata auditMetadata = buildAuditMetadata();
        return MembersGroup.reconstruct(new MembersGroupId(this.id), this.name, ownerIds, memberships, invitationSet, auditMetadata);
    }

    public TrainingGroup toTrainingGroup() {
        Set<MemberId> trainerIds = owners.stream()
                .map(o -> new MemberId(o.getMemberId()))
                .collect(Collectors.toSet());

        Set<GroupMembership> memberships = members.stream()
                .map(m -> new GroupMembership(new UserId(m.getMemberId()), m.getJoinedAt()))
                .collect(Collectors.toSet());

        AuditMetadata auditMetadata = buildAuditMetadata();
        return TrainingGroup.reconstruct(new TrainingGroupId(this.id), this.name, trainerIds, memberships,
                new AgeRange(this.ageRangeMin, this.ageRangeMax), auditMetadata);
    }

    public FamilyGroup toFamilyGroup() {
        Set<MemberId> parentIds = owners.stream()
                .map(o -> new MemberId(o.getMemberId()))
                .collect(Collectors.toSet());

        Set<GroupMembership> memberships = members.stream()
                .map(m -> new GroupMembership(new UserId(m.getMemberId()), m.getJoinedAt()))
                .collect(Collectors.toSet());

        AuditMetadata auditMetadata = buildAuditMetadata();
        return FamilyGroup.reconstruct(new FamilyGroupId(this.id), this.name, parentIds, memberships, auditMetadata);
    }

    @DomainEvents
    public List<Object> getDomainEvents() {
        if (aggregate instanceof MembersGroup membersGroup) {
            return membersGroup.getDomainEvents();
        }
        if (aggregate instanceof TrainingGroup trainingGroup) {
            return trainingGroup.getDomainEvents();
        }
        if (aggregate instanceof FamilyGroup familyGroup) {
            return familyGroup.getDomainEvents();
        }
        return List.of();
    }

    @AfterDomainEventPublication
    public void clearDomainEvents() {
        if (aggregate instanceof MembersGroup membersGroup) {
            membersGroup.clearDomainEvents();
        } else if (aggregate instanceof TrainingGroup trainingGroup) {
            trainingGroup.clearDomainEvents();
        } else if (aggregate instanceof FamilyGroup familyGroup) {
            familyGroup.clearDomainEvents();
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

    private void applyAudit(AuditMetadata auditMetadata) {
        if (auditMetadata != null) {
            this.createdAt = auditMetadata.createdAt();
            this.createdBy = auditMetadata.createdBy();
            this.lastModifiedAt = auditMetadata.lastModifiedAt();
            this.lastModifiedBy = auditMetadata.lastModifiedBy();
            this.version = auditMetadata.version();
        }
    }

    private AuditMetadata buildAuditMetadata() {
        return this.createdAt != null
                ? new AuditMetadata(this.createdAt, this.createdBy, this.lastModifiedAt, this.lastModifiedBy, this.version)
                : null;
    }
}
