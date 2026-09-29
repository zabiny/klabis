package com.klabis.groups.freegroup.infrastructure.jdbc;

import com.klabis.common.groups.infrastructure.jdbc.GroupInvitationMemento;
import com.klabis.common.groups.infrastructure.jdbc.GroupJdbcRepository;
import com.klabis.common.groups.infrastructure.jdbc.GroupMemento;
import com.klabis.groups.freegroup.FreeGroupId;
import com.klabis.groups.freegroup.domain.*;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@SecondaryAdapter
@Repository
class FreeGroupRepositoryAdapter implements FreeGroupRepository {

    private final GroupJdbcRepository jdbcRepository;
    private final JdbcAggregateTemplate jdbcAggregateTemplate;

    FreeGroupRepositoryAdapter(GroupJdbcRepository jdbcRepository,
                                   JdbcAggregateTemplate jdbcAggregateTemplate) {
        this.jdbcRepository = jdbcRepository;
        this.jdbcAggregateTemplate = jdbcAggregateTemplate;
    }

    @Override
    public FreeGroup save(FreeGroup group) {
        return toDomain(jdbcRepository.save(fromDomain(group)));
    }

    @Override
    public Optional<FreeGroup> findById(FreeGroupId id) {
        return jdbcRepository.findByIdAndType(id.value(), FreeGroup.TYPE_DISCRIMINATOR)
                .map(this::toDomain);
    }

    @Override
    public List<FreeGroup> findAll(FreeGroupFilter filter) {
        return buildQuery(filter)
                .map(query -> jdbcAggregateTemplate.findAll(query, GroupMemento.class)
                        .stream().map(this::toDomain).toList())
                .orElseGet(() -> findAllByComplexFilter(filter));
    }

    @Override
    public Optional<FreeGroup> findOne(FreeGroupFilter filter) {
        List<GroupMemento> results = buildQuery(filter)
                .map(query -> jdbcAggregateTemplate.findAll(query.limit(2), GroupMemento.class))
                .orElseGet(() -> findFirst2MementosByComplexFilter(filter));

        if (results.size() > 1) {
            throw new IllegalStateException(
                    "findOne expected at most 1 result but filter matched " + results.size() + " rows");
        }
        return results.stream().findFirst().map(this::toDomain);
    }

    @Override
    public boolean exists(FreeGroupFilter filter) {
        return buildQuery(filter)
                .map(query -> jdbcAggregateTemplate.count(query, GroupMemento.class) > 0)
                .orElseGet(() -> !findAllMementosByComplexFilter(filter).isEmpty());
    }

    @Override
    public boolean existsById(FreeGroupId id) {
        return jdbcRepository.existsByIdAndType(id.value(), FreeGroup.TYPE_DISCRIMINATOR);
    }

    @Override
    public void delete(FreeGroupId id) {
        jdbcRepository.deleteByIdAndType(id.value(), FreeGroup.TYPE_DISCRIMINATOR);
    }

    /**
     * Builds a {@link Query} using {@link Criteria} for filter fields that can be expressed
     * as simple column conditions. Returns empty when the filter requires complex EXISTS
     * sub-queries ({@code ownerOrMemberIs} or {@code pendingInvitationFor}), in which case
     * the caller falls back to {@link GroupJdbcRepository} named queries.
     */
    private Optional<Query> buildQuery(FreeGroupFilter filter) {
        if (filter.ownerOrMemberIs() != null || filter.pendingInvitationFor() != null) {
            return Optional.empty();
        }
        return Optional.of(Query.query(Criteria.where("type").is(FreeGroup.TYPE_DISCRIMINATOR)));
    }

    private List<FreeGroup> findAllByComplexFilter(FreeGroupFilter filter) {
        return findAllMementosByComplexFilter(filter)
                .stream().map(this::toDomain).toList();
    }

    /**
     * Handles filter cases that require EXISTS sub-queries or JOINs that the Criteria API
     * cannot express. Falls back to existing named {@link GroupJdbcRepository} query methods
     * which already include the type discriminator. Returns all matching rows — used by the
     * {@code findAll} path only.
     */
    private List<GroupMemento> findAllMementosByComplexFilter(FreeGroupFilter filter) {
        if (filter.ownerOrMemberIs() != null && filter.pendingInvitationFor() != null) {
            throw new UnsupportedOperationException(
                    "Combining ownerOrMemberIs and pendingInvitationFor in a single filter is not supported");
        }
        if (filter.ownerOrMemberIs() != null) {
            return jdbcRepository.findOwnersOrMembersByType(
                    filter.ownerOrMemberIs().value(), FreeGroup.TYPE_DISCRIMINATOR);
        }
        if (filter.pendingInvitationFor() != null) {
            return jdbcRepository.findWithPendingInvitationsByType(
                    filter.pendingInvitationFor().value(), FreeGroup.TYPE_DISCRIMINATOR);
        }
        throw new IllegalStateException("Unexpected empty complex filter — should have used buildQuery path");
    }

    /**
     * SQL-level LIMIT 2 variant for the {@code findOne} path — avoids loading all rows
     * when a business invariant is violated and the filter unexpectedly matches multiple rows.
     */
    private List<GroupMemento> findFirst2MementosByComplexFilter(FreeGroupFilter filter) {
        if (filter.ownerOrMemberIs() != null && filter.pendingInvitationFor() != null) {
            throw new UnsupportedOperationException(
                    "Combining ownerOrMemberIs and pendingInvitationFor in a single filter is not supported");
        }
        if (filter.ownerOrMemberIs() != null) {
            return jdbcRepository.findFirst2OwnersOrMembersByType(
                    filter.ownerOrMemberIs().value(), FreeGroup.TYPE_DISCRIMINATOR);
        }
        if (filter.pendingInvitationFor() != null) {
            return jdbcRepository.findFirst2WithPendingInvitationsByType(
                    filter.pendingInvitationFor().value(), FreeGroup.TYPE_DISCRIMINATOR);
        }
        throw new IllegalStateException("Unexpected empty complex filter — should have used buildQuery path");
    }

    private GroupMemento fromDomain(FreeGroup group) {
        return GroupMemento.from(group, group.getId().value(), FreeGroup.TYPE_DISCRIMINATOR, MemberId::value)
                .withInvitations(group.getInvitations().stream()
                        .map(inv -> new GroupInvitationMemento(
                                inv.getId().value(),
                                inv.getInvitedMember().value(),
                                inv.getInvitedBy().value(),
                                inv.getStatus().name(),
                                inv.getCreatedAt(),
                                inv.getCancelledAt().orElse(null),
                                inv.getCancelledBy().map(MemberId::value).orElse(null),
                                inv.getCancellationReason().orElse(null)))
                        .collect(Collectors.toSet()));
    }

    private FreeGroup toDomain(GroupMemento memento) {
        Set<Invitation> invitations = memento.getInvitations().stream()
                .map(inv -> Invitation.reconstruct(
                        new InvitationId(inv.getId()),
                        new MemberId(inv.getInvitedMemberId()),
                        new MemberId(inv.getInvitedByMemberId()),
                        InvitationStatus.valueOf(inv.getStatus()),
                        inv.getCreatedAt(),
                        inv.getCancelledAt(),
                        inv.getCancelledBy() != null ? new MemberId(inv.getCancelledBy()) : null,
                        inv.getCancellationReason()))
                .collect(Collectors.toSet());
        return FreeGroup.reconstruct(new FreeGroupId(memento.getId()), memento.getName(),
                memento.ownerIds(MemberId::new), memento.memberships(MemberId::new), invitations,
                memento.auditMetadata());
    }
}
