package com.klabis.members.familygroup.infrastructure.jdbc;

import com.klabis.common.groups.infrastructure.jdbc.GroupJdbcRepository;
import com.klabis.common.groups.infrastructure.jdbc.GroupMemento;
import com.klabis.common.users.UserId;
import com.klabis.members.familygroup.FamilyGroupId;
import com.klabis.members.familygroup.domain.FamilyGroup;
import com.klabis.members.familygroup.domain.FamilyGroupFilter;
import com.klabis.members.familygroup.domain.FamilyGroupRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;

import java.util.List;
import java.util.Optional;

@SecondaryAdapter
@Repository
class FamilyGroupRepositoryAdapter implements FamilyGroupRepository {

    private final GroupJdbcRepository jdbcRepository;
    private final JdbcAggregateTemplate jdbcAggregateTemplate;

    FamilyGroupRepositoryAdapter(GroupJdbcRepository jdbcRepository,
                                  JdbcAggregateTemplate jdbcAggregateTemplate) {
        this.jdbcRepository = jdbcRepository;
        this.jdbcAggregateTemplate = jdbcAggregateTemplate;
    }

    @Override
    public FamilyGroup save(FamilyGroup group) {
        return toDomain(jdbcRepository.save(fromDomain(group)));
    }

    @Override
    public Optional<FamilyGroup> findById(FamilyGroupId id) {
        return jdbcRepository.findByIdAndType(id.value(), FamilyGroup.TYPE_DISCRIMINATOR)
                .map(this::toDomain);
    }

    @Override
    public List<FamilyGroup> findAll(FamilyGroupFilter filter) {
        return buildSimpleCriteriaQuery(filter)
                .map(query -> jdbcAggregateTemplate.findAll(query, GroupMemento.class)
                        .stream().map(this::toDomain).toList())
                .orElseGet(() -> findAllByComplexFilter(filter));
    }

    @Override
    public Optional<FamilyGroup> findOne(FamilyGroupFilter filter) {
        List<GroupMemento> results = buildSimpleCriteriaQuery(filter)
                .map(query -> jdbcAggregateTemplate.findAll(query.limit(2), GroupMemento.class))
                .orElseGet(() -> findFirst2MementosByComplexFilter(filter));

        if (results.size() > 1) {
            throw new IllegalStateException(
                    "findOne expected at most 1 result but filter matched " + results.size() + " rows");
        }
        return results.stream().findFirst().map(this::toDomain);
    }

    @Override
    public boolean exists(FamilyGroupFilter filter) {
        return buildSimpleCriteriaQuery(filter)
                .map(query -> jdbcAggregateTemplate.count(query, GroupMemento.class) > 0)
                .orElseGet(() -> !findAllMementosByComplexFilter(filter).isEmpty());
    }

    @Override
    public void delete(FamilyGroupId id) {
        jdbcRepository.deleteByIdAndType(id.value(), FamilyGroup.TYPE_DISCRIMINATOR);
    }

    /**
     * Builds a {@link Query} using {@link Criteria} for the discriminator-only case (filter.all()).
     * Returns empty when the filter requires an EXISTS sub-query ({@code memberOrParentIs}),
     * in which case callers fall back to named {@link GroupJdbcRepository} queries.
     */
    private Optional<Query> buildSimpleCriteriaQuery(FamilyGroupFilter filter) {
        if (filter.memberOrParentIs() != null) {
            return Optional.empty();
        }
        return Optional.of(Query.query(Criteria.where("type").is(FamilyGroup.TYPE_DISCRIMINATOR)));
    }

    private List<FamilyGroup> findAllByComplexFilter(FamilyGroupFilter filter) {
        return findAllMementosByComplexFilter(filter)
                .stream().map(this::toDomain).toList();
    }

    /**
     * Handles the {@code memberOrParentIs} case which requires EXISTS sub-queries over
     * {@code user_group_owners} (parents) and {@code user_group_members} (children).
     * Returns all matching rows — used by the {@code findAll} and {@code exists} paths only.
     */
    private List<GroupMemento> findAllMementosByComplexFilter(FamilyGroupFilter filter) {
        if (filter.memberOrParentIs() != null) {
            return jdbcRepository.findOwnersOrMembersByType(
                    filter.memberOrParentIs().uuid(), FamilyGroup.TYPE_DISCRIMINATOR);
        }
        throw new IllegalStateException("Unexpected empty complex filter — should have used buildSimpleCriteriaQuery path");
    }

    /**
     * SQL-level LIMIT 2 variant for the {@code findOne} path — avoids loading all rows
     * when a business invariant is violated and the filter unexpectedly matches multiple rows.
     */
    private List<GroupMemento> findFirst2MementosByComplexFilter(FamilyGroupFilter filter) {
        if (filter.memberOrParentIs() != null) {
            return jdbcRepository.findFirst2OwnersOrMembersByType(
                    filter.memberOrParentIs().uuid(), FamilyGroup.TYPE_DISCRIMINATOR);
        }
        throw new IllegalStateException("Unexpected empty complex filter — should have used buildSimpleCriteriaQuery path");
    }

    private GroupMemento fromDomain(FamilyGroup group) {
        return GroupMemento.from(group, group.getId().value(), FamilyGroup.TYPE_DISCRIMINATOR, UserId::uuid);
    }

    private FamilyGroup toDomain(GroupMemento memento) {
        return FamilyGroup.reconstruct(new FamilyGroupId(memento.getId()), memento.getName(),
                memento.ownerIds(UserId::new), memento.memberships(UserId::new), memento.auditMetadata());
    }
}
