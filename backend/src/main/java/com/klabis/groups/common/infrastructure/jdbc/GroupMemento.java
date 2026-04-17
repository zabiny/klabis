package com.klabis.groups.common.infrastructure.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.domain.KlabisAggregateRoot;
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
    private KlabisAggregateRoot<?, ?> aggregate;

    @Transient
    private boolean isNew = true;

    protected GroupMemento() {
    }

    public static GroupMemento fromMembersGroup(MembersGroup group) {
        GroupMemento memento = initCommon(group, group.getId().value(), group.getName(), MembersGroup.TYPE_DISCRIMINATOR);
        memento.owners = mapOwners(group.getOwners());
        memento.members = mapMembershipsToMementa(group.getMembers());
        memento.invitations = group.getInvitations().stream()
                .map(inv -> new GroupInvitationMemento(
                        inv.getId().value(),
                        inv.getInvitedUser().uuid(),
                        inv.getInvitedBy().uuid(),
                        inv.getStatus().name(),
                        inv.getCreatedAt()))
                .collect(Collectors.toSet());
        return memento;
    }

    public static GroupMemento fromTrainingGroup(TrainingGroup group) {
        GroupMemento memento = initCommon(group, group.getId().value(), group.getName(), TrainingGroup.TYPE_DISCRIMINATOR);
        memento.ageRangeMin = group.getAgeRange().minAge();
        memento.ageRangeMax = group.getAgeRange().maxAge();
        memento.owners = mapOwners(group.getTrainers());
        memento.members = mapMembershipsToMementa(group.getMembers());
        return memento;
    }

    public static GroupMemento fromFamilyGroup(FamilyGroup group) {
        GroupMemento memento = initCommon(group, group.getId().value(), group.getName(), FamilyGroup.TYPE_DISCRIMINATOR);
        memento.owners = mapOwners(group.getParents());
        memento.members = mapMembershipsToMementa(group.getMembers());
        return memento;
    }

    public MembersGroup toMembersGroup() {
        Set<Invitation> invitationSet = invitations.stream()
                .map(inv -> Invitation.reconstruct(
                        new InvitationId(inv.getId()),
                        new UserId(inv.getInvitedMemberId()),
                        new UserId(inv.getInvitedByMemberId()),
                        InvitationStatus.valueOf(inv.getStatus()),
                        inv.getCreatedAt()))
                .collect(Collectors.toSet());

        return MembersGroup.reconstruct(new MembersGroupId(this.id), this.name,
                mapOwnerIds(), mapMemberships(), invitationSet, buildAuditMetadata());
    }

    public TrainingGroup toTrainingGroup() {
        return TrainingGroup.reconstruct(new TrainingGroupId(this.id), this.name,
                mapOwnerIds(), mapMemberships(),
                new AgeRange(this.ageRangeMin, this.ageRangeMax), buildAuditMetadata());
    }

    public FamilyGroup toFamilyGroup() {
        return FamilyGroup.reconstruct(new FamilyGroupId(this.id), this.name,
                mapOwnerIds(), mapMemberships(), buildAuditMetadata());
    }

    @DomainEvents
    public List<Object> getDomainEvents() {
        return aggregate != null ? aggregate.getDomainEvents() : List.of();
    }

    @AfterDomainEventPublication
    public void clearDomainEvents() {
        if (aggregate != null) {
            aggregate.clearDomainEvents();
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

    private static GroupMemento initCommon(KlabisAggregateRoot<?, ?> group, UUID id, String name, String type) {
        GroupMemento memento = new GroupMemento();
        memento.id = id;
        memento.name = name;
        memento.type = type;
        memento.aggregate = group;
        memento.isNew = (group.getAuditMetadata() == null);
        memento.applyAudit(group.getAuditMetadata());
        return memento;
    }

    private Set<MemberId> mapOwnerIds() {
        return owners.stream()
                .map(o -> new MemberId(o.getMemberId()))
                .collect(Collectors.toSet());
    }

    private Set<GroupMembership> mapMemberships() {
        return members.stream()
                .map(m -> new GroupMembership(new UserId(m.getMemberId()), m.getJoinedAt()))
                .collect(Collectors.toSet());
    }

    private static Set<GroupOwnerMemento> mapOwners(Set<MemberId> source) {
        return source.stream()
                .map(memberId -> new GroupOwnerMemento(memberId.value()))
                .collect(Collectors.toSet());
    }

    private static Set<GroupMemberMemento> mapMembershipsToMementa(Set<GroupMembership> source) {
        return source.stream()
                .map(m -> new GroupMemberMemento(m.userId().uuid(), m.joinedAt()))
                .collect(Collectors.toSet());
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
