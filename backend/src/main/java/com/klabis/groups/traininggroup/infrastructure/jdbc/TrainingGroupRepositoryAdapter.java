package com.klabis.groups.traininggroup.infrastructure.jdbc;

import com.klabis.groups.common.domain.AgeRangeOverlap;
import com.klabis.groups.common.domain.TrainingGroupFilter;
import com.klabis.groups.common.infrastructure.jdbc.GroupJdbcRepository;
import com.klabis.groups.common.infrastructure.jdbc.GroupMemento;
import com.klabis.groups.traininggroup.TrainingGroupId;
import com.klabis.groups.traininggroup.domain.TrainingGroup;
import com.klabis.groups.traininggroup.domain.TrainingGroupRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;

import java.util.List;
import java.util.Optional;

@SecondaryAdapter
@Repository
class TrainingGroupRepositoryAdapter implements TrainingGroupRepository {

    private final GroupJdbcRepository jdbcRepository;
    private final JdbcAggregateTemplate jdbcAggregateTemplate;

    TrainingGroupRepositoryAdapter(GroupJdbcRepository jdbcRepository,
                                   JdbcAggregateTemplate jdbcAggregateTemplate) {
        this.jdbcRepository = jdbcRepository;
        this.jdbcAggregateTemplate = jdbcAggregateTemplate;
    }

    @Override
    public TrainingGroup save(TrainingGroup group) {
        return jdbcRepository.save(GroupMemento.fromTrainingGroup(group)).toTrainingGroup();
    }

    @Override
    public Optional<TrainingGroup> findById(TrainingGroupId id) {
        return jdbcRepository.findByIdAndType(id.value(), TrainingGroup.TYPE_DISCRIMINATOR)
                .map(GroupMemento::toTrainingGroup);
    }

    @Override
    public List<TrainingGroup> findAll(TrainingGroupFilter filter) {
        return buildSimpleCriteriaQuery(filter)
                .map(query -> jdbcAggregateTemplate.findAll(query, GroupMemento.class)
                        .stream().map(GroupMemento::toTrainingGroup).toList())
                .orElseGet(() -> findAllByComplexFilter(filter));
    }

    @Override
    public Optional<TrainingGroup> findOne(TrainingGroupFilter filter) {
        List<GroupMemento> results = buildSimpleCriteriaQuery(filter)
                .map(query -> jdbcAggregateTemplate.findAll(query.limit(2), GroupMemento.class))
                .orElseGet(() -> findFirst2MementosByComplexFilter(filter));

        if (results.size() > 1) {
            throw new IllegalStateException(
                    "findOne expected at most 1 result but filter matched " + results.size() + " rows");
        }
        return results.stream().findFirst().map(GroupMemento::toTrainingGroup);
    }

    @Override
    public boolean exists(TrainingGroupFilter filter) {
        return buildSimpleCriteriaQuery(filter)
                .map(query -> jdbcAggregateTemplate.count(query, GroupMemento.class) > 0)
                .orElseGet(() -> !findAllMementosByComplexFilter(filter).isEmpty());
    }

    @Override
    public void delete(TrainingGroupId id) {
        jdbcRepository.deleteByIdAndType(id.value(), TrainingGroup.TYPE_DISCRIMINATOR);
    }

    /**
     * Builds a {@link Query} using {@link Criteria} for filters that can be expressed as simple
     * column conditions (no JOIN required). Returns empty when the filter requires a JOIN
     * ({@code memberIs} or {@code trainerIs}), in which case callers fall back to named
     * {@link GroupJdbcRepository} queries. The {@code overlap} predicate can always be expressed
     * via Criteria — simple numeric column comparisons, no EXISTS subquery needed.
     */
    private Optional<Query> buildSimpleCriteriaQuery(TrainingGroupFilter filter) {
        if (filter.memberIs() != null || filter.trainerIs() != null) {
            return Optional.empty();
        }
        Criteria criteria = Criteria.where("type").is(TrainingGroup.TYPE_DISCRIMINATOR);
        if (filter.overlap() != null) {
            criteria = criteria.and(applyOverlapCriteria(filter.overlap()));
        }
        return Optional.of(Query.query(criteria));
    }

    private Criteria applyOverlapCriteria(AgeRangeOverlap overlap) {
        Criteria ageCriteria = Criteria.where("age_range_min").lessThanOrEquals(overlap.range().maxAge())
                .and("age_range_max").greaterThanOrEquals(overlap.range().minAge());
        if (overlap.excludeId() != null) {
            return ageCriteria.and(Criteria.where("id").not(overlap.excludeId().value()));
        }
        return ageCriteria;
    }

    private List<TrainingGroup> findAllByComplexFilter(TrainingGroupFilter filter) {
        return findAllMementosByComplexFilter(filter)
                .stream().map(GroupMemento::toTrainingGroup).toList();
    }

    /**
     * Handles filter cases requiring JOINs on {@code user_group_members} or {@code user_group_owners}.
     * Falls back to named {@link GroupJdbcRepository} queries which already include the type discriminator.
     * Returns all matching rows — used by the {@code findAll} and {@code exists} paths only.
     */
    private List<GroupMemento> findAllMementosByComplexFilter(TrainingGroupFilter filter) {
        if (filter.memberIs() != null && filter.trainerIs() != null) {
            throw new UnsupportedOperationException(
                    "Combining memberIs and trainerIs in a single filter is not supported");
        }
        if (filter.memberIs() != null) {
            return jdbcRepository.findByMemberIdAndType(filter.memberIs().value(), TrainingGroup.TYPE_DISCRIMINATOR);
        }
        if (filter.trainerIs() != null) {
            return jdbcRepository.findByTrainerIdAndType(filter.trainerIs().value(), TrainingGroup.TYPE_DISCRIMINATOR);
        }
        throw new IllegalStateException("Unexpected empty complex filter — should have used buildSimpleCriteriaQuery path");
    }

    /**
     * SQL-level LIMIT 2 variant for the {@code findOne} path — avoids loading all rows
     * when a business invariant is violated and the filter unexpectedly matches multiple rows.
     */
    private List<GroupMemento> findFirst2MementosByComplexFilter(TrainingGroupFilter filter) {
        if (filter.memberIs() != null && filter.trainerIs() != null) {
            throw new UnsupportedOperationException(
                    "Combining memberIs and trainerIs in a single filter is not supported");
        }
        if (filter.memberIs() != null) {
            return jdbcRepository.findFirst2ByMemberIdAndType(filter.memberIs().value(), TrainingGroup.TYPE_DISCRIMINATOR);
        }
        if (filter.trainerIs() != null) {
            return jdbcRepository.findFirst2ByTrainerIdAndType(filter.trainerIs().value(), TrainingGroup.TYPE_DISCRIMINATOR);
        }
        throw new IllegalStateException("Unexpected empty complex filter — should have used buildSimpleCriteriaQuery path");
    }
}
