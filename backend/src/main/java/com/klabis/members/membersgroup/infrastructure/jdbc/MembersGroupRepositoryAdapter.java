package com.klabis.members.membersgroup.infrastructure.jdbc;

import com.klabis.groups.common.domain.MembersGroupFilter;
import com.klabis.groups.common.infrastructure.jdbc.GroupJdbcRepository;
import com.klabis.groups.common.infrastructure.jdbc.GroupMemento;
import com.klabis.members.membersgroup.domain.MembersGroup;
import com.klabis.members.membersgroup.domain.MembersGroupId;
import com.klabis.members.membersgroup.domain.MembersGroupRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;

import java.util.List;
import java.util.Optional;

@SecondaryAdapter
@Repository
class MembersGroupRepositoryAdapter implements MembersGroupRepository {

    private final GroupJdbcRepository jdbcRepository;
    private final JdbcAggregateTemplate jdbcAggregateTemplate;

    MembersGroupRepositoryAdapter(GroupJdbcRepository jdbcRepository,
                                   JdbcAggregateTemplate jdbcAggregateTemplate) {
        this.jdbcRepository = jdbcRepository;
        this.jdbcAggregateTemplate = jdbcAggregateTemplate;
    }

    @Override
    public MembersGroup save(MembersGroup group) {
        return jdbcRepository.save(GroupMemento.fromMembersGroup(group)).toMembersGroup();
    }

    @Override
    public Optional<MembersGroup> findById(MembersGroupId id) {
        return jdbcRepository.findByIdAndType(id.value(), MembersGroup.TYPE_DISCRIMINATOR)
                .map(GroupMemento::toMembersGroup);
    }

    @Override
    public List<MembersGroup> findAll(MembersGroupFilter filter) {
        return buildQuery(filter)
                .map(query -> jdbcAggregateTemplate.findAll(query, GroupMemento.class)
                        .stream().map(GroupMemento::toMembersGroup).toList())
                .orElseGet(() -> findAllByComplexFilter(filter));
    }

    @Override
    public Optional<MembersGroup> findOne(MembersGroupFilter filter) {
        List<GroupMemento> results = buildQuery(filter)
                .map(query -> jdbcAggregateTemplate.findAll(query.limit(2), GroupMemento.class))
                .orElseGet(() -> findFirst2MementosByComplexFilter(filter));

        if (results.size() > 1) {
            throw new IllegalStateException(
                    "findOne expected at most 1 result but filter matched " + results.size() + " rows");
        }
        return results.stream().findFirst().map(GroupMemento::toMembersGroup);
    }

    @Override
    public boolean exists(MembersGroupFilter filter) {
        return buildQuery(filter)
                .map(query -> jdbcAggregateTemplate.count(query, GroupMemento.class) > 0)
                .orElseGet(() -> !findAllMementosByComplexFilter(filter).isEmpty());
    }

    @Override
    public boolean existsById(MembersGroupId id) {
        return jdbcRepository.existsByIdAndType(id.value(), MembersGroup.TYPE_DISCRIMINATOR);
    }

    @Override
    public void delete(MembersGroupId id) {
        jdbcRepository.deleteByIdAndType(id.value(), MembersGroup.TYPE_DISCRIMINATOR);
    }

    /**
     * Builds a {@link Query} using {@link Criteria} for filter fields that can be expressed
     * as simple column conditions. Returns empty when the filter requires complex EXISTS
     * sub-queries ({@code ownerOrMemberIs} or {@code pendingInvitationFor}), in which case
     * the caller falls back to {@link GroupJdbcRepository} named queries.
     */
    private Optional<Query> buildQuery(MembersGroupFilter filter) {
        if (filter.ownerOrMemberIs() != null || filter.pendingInvitationFor() != null) {
            return Optional.empty();
        }
        return Optional.of(Query.query(Criteria.where("type").is(MembersGroup.TYPE_DISCRIMINATOR)));
    }

    private List<MembersGroup> findAllByComplexFilter(MembersGroupFilter filter) {
        return findAllMementosByComplexFilter(filter)
                .stream().map(GroupMemento::toMembersGroup).toList();
    }

    /**
     * Handles filter cases that require EXISTS sub-queries or JOINs that the Criteria API
     * cannot express. Falls back to existing named {@link GroupJdbcRepository} query methods
     * which already include the type discriminator. Returns all matching rows — used by the
     * {@code findAll} path only.
     */
    private List<GroupMemento> findAllMementosByComplexFilter(MembersGroupFilter filter) {
        if (filter.ownerOrMemberIs() != null && filter.pendingInvitationFor() != null) {
            throw new UnsupportedOperationException(
                    "Combining ownerOrMemberIs and pendingInvitationFor in a single filter is not supported");
        }
        if (filter.ownerOrMemberIs() != null) {
            return jdbcRepository.findOwnersOrMembersByType(
                    filter.ownerOrMemberIs().value(), MembersGroup.TYPE_DISCRIMINATOR);
        }
        if (filter.pendingInvitationFor() != null) {
            return jdbcRepository.findWithPendingInvitationsByType(
                    filter.pendingInvitationFor().value(), MembersGroup.TYPE_DISCRIMINATOR);
        }
        throw new IllegalStateException("Unexpected empty complex filter — should have used buildQuery path");
    }

    /**
     * SQL-level LIMIT 2 variant for the {@code findOne} path — avoids loading all rows
     * when a business invariant is violated and the filter unexpectedly matches multiple rows.
     */
    private List<GroupMemento> findFirst2MementosByComplexFilter(MembersGroupFilter filter) {
        if (filter.ownerOrMemberIs() != null && filter.pendingInvitationFor() != null) {
            throw new UnsupportedOperationException(
                    "Combining ownerOrMemberIs and pendingInvitationFor in a single filter is not supported");
        }
        if (filter.ownerOrMemberIs() != null) {
            return jdbcRepository.findFirst2OwnersOrMembersByType(
                    filter.ownerOrMemberIs().value(), MembersGroup.TYPE_DISCRIMINATOR);
        }
        if (filter.pendingInvitationFor() != null) {
            return jdbcRepository.findFirst2WithPendingInvitationsByType(
                    filter.pendingInvitationFor().value(), MembersGroup.TYPE_DISCRIMINATOR);
        }
        throw new IllegalStateException("Unexpected empty complex filter — should have used buildQuery path");
    }
}
