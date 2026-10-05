package com.klabis.common.groups.infrastructure.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.common.groups.domain.GroupMembership;
import com.klabis.common.groups.domain.MemberGroup;
import com.klabis.common.users.Authority;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Table(schema = "groups", value = "user_groups")
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

    @Column("delegated_authorities")
    private String delegatedAuthorities;

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

    public static <M> GroupMemento from(MemberGroup<?, ?, M> group, UUID id, String type, Function<M, UUID> memberIdToUuid) {
        GroupMemento memento = initCommon(group, id, group.getName(), type);
        memento.owners = group.getOwners().stream()
                .map(owner -> new GroupOwnerMemento(memberIdToUuid.apply(owner)))
                .collect(Collectors.toSet());
        memento.members = group.getMembers().stream()
                .map(m -> new GroupMemberMemento(memberIdToUuid.apply(m.memberId()), m.joinedAt()))
                .collect(Collectors.toSet());
        return memento;
    }

    public <M> Set<M> ownerIds(Function<UUID, M> uuidToMemberId) {
        return owners.stream()
                .map(o -> uuidToMemberId.apply(o.getOwnerId()))
                .collect(Collectors.toSet());
    }

    public <M> Set<GroupMembership<M>> memberships(Function<UUID, M> uuidToMemberId) {
        return members.stream()
                .map(m -> new GroupMembership<>(uuidToMemberId.apply(m.getMemberId()), m.getJoinedAt()))
                .collect(Collectors.toSet());
    }

    public String getName() {
        return name;
    }

    public Integer getAgeRangeMin() {
        return ageRangeMin;
    }

    public Integer getAgeRangeMax() {
        return ageRangeMax;
    }

    public GroupMemento withAgeRange(Integer min, Integer max) {
        this.ageRangeMin = min;
        this.ageRangeMax = max;
        return this;
    }

    /**
     * Stored as a comma-separated list of authority values; only free groups carry any, so the column is
     * null for the other group types.
     */
    public Set<Authority> delegatedAuthorities() {
        if (delegatedAuthorities == null || delegatedAuthorities.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(delegatedAuthorities.split(","))
                .map(Authority::fromString)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Authority.class)));
    }

    public GroupMemento withDelegatedAuthorities(Set<Authority> authorities) {
        this.delegatedAuthorities = authorities.isEmpty()
                ? null
                : authorities.stream().map(Authority::getValue).sorted().collect(Collectors.joining(","));
        return this;
    }

    public Set<GroupInvitationMemento> getInvitations() {
        return invitations;
    }

    public GroupMemento withInvitations(Set<GroupInvitationMemento> invitations) {
        this.invitations = invitations;
        return this;
    }

    public AuditMetadata auditMetadata() {
        return buildAuditMetadata();
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
