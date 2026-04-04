package com.klabis.members.traininggroup.infrastructure.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.usergroup.GroupMembership;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
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

@Table("training_groups")
class TrainingGroupMemento implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("name")
    private String name;

    @Column("age_range_min")
    private int ageRangeMin;

    @Column("age_range_max")
    private int ageRangeMax;

    @MappedCollection(idColumn = "training_group_id")
    private Set<TrainingGroupTrainerMemento> trainers = new HashSet<>();

    @MappedCollection(idColumn = "training_group_id")
    private Set<TrainingGroupMemberMemento> members = new HashSet<>();

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
    private TrainingGroup trainingGroup;

    @Transient
    private boolean isNew = true;

    protected TrainingGroupMemento() {
    }

    static TrainingGroupMemento from(TrainingGroup group) {
        TrainingGroupMemento memento = new TrainingGroupMemento();
        memento.id = group.getId().value();
        memento.name = group.getName();
        memento.ageRangeMin = group.getAgeRange().minAge();
        memento.ageRangeMax = group.getAgeRange().maxAge();

        memento.trainers = group.getTrainers().stream()
                .map(memberId -> new TrainingGroupTrainerMemento(memberId.uuid()))
                .collect(Collectors.toSet());

        memento.members = group.getMembers().stream()
                .map(m -> new TrainingGroupMemberMemento(m.userId().uuid(), m.joinedAt()))
                .collect(Collectors.toSet());

        if (group.getAuditMetadata() != null) {
            memento.createdAt = group.getCreatedAt();
            memento.createdBy = group.getCreatedBy();
            memento.lastModifiedAt = group.getLastModifiedAt();
            memento.lastModifiedBy = group.getLastModifiedBy();
            memento.version = group.getVersion();
        }

        memento.trainingGroup = group;
        memento.isNew = (group.getAuditMetadata() == null);
        return memento;
    }

    TrainingGroup toTrainingGroup() {
        TrainingGroupId groupId = new TrainingGroupId(this.id);

        Set<MemberId> trainerIds = trainers.stream()
                .map(t -> new MemberId(t.getMemberId()))
                .collect(Collectors.toSet());

        Set<GroupMembership> memberships = members.stream()
                .map(m -> new GroupMembership(new UserId(m.getMemberId()), m.getJoinedAt()))
                .collect(Collectors.toSet());

        AuditMetadata auditMetadata = this.createdAt != null
                ? new AuditMetadata(this.createdAt, this.createdBy, this.lastModifiedAt, this.lastModifiedBy, this.version)
                : null;

        return TrainingGroup.reconstruct(groupId, this.name, trainerIds, memberships,
                new AgeRange(this.ageRangeMin, this.ageRangeMax), auditMetadata);
    }

    @DomainEvents
    public List<Object> getDomainEvents() {
        return this.trainingGroup != null ? this.trainingGroup.getDomainEvents() : List.of();
    }

    @AfterDomainEventPublication
    public void clearDomainEvents() {
        if (this.trainingGroup != null) {
            this.trainingGroup.clearDomainEvents();
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
